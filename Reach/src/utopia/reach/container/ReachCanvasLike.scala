package utopia.reach.container

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.genesis.graphics.{PaintManager, Priority}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.template.ReachComponent
import utopia.reach.cursor.ReachCursorManager
import utopia.reach.focus.ReachFocusManager

import scala.collection.immutable.VectorBuilder

/**
  * A common trait for canvas elements where components are drawn
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
trait ReachCanvasLike
{
	// ABSTRACT	-------------------------------
	
	/**
	  * @return The currently managed / displayed component. None while not set up.
	  */
	protected def currentContent: Option[ReachComponent]
	
	/**
	  * @return A painter that will draw canvas content. None while not set up.
	  */
	protected def currentPainter: Option[PaintManager]
	
	/**
	  * Object that manages focus between the components in this canvas element
	  */
	def focusManager: ReachFocusManager
	
	/**
	  * Object that manages cursor display inside this canvas. None if cursor state is not managed in this canvas.
	  */
	def cursorManager: Option[ReachCursorManager]
	
	/**
	  * Revalidates this component, queueing some component layout updates to be done afterwards
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  */
	def revalidate(updateComponents: Seq[ReachComponent]): Unit
	
	/**
	  * Revalidates this component's layout. Calls the specified function when whole component layout has been updated.
	  * @param updateComponents Sequence of components from hierarchy top downwards that require a layout update once
	  *                         this canvas has been revalidated
	  * @param f A function called after layout has been updated.
	  */
	def revalidateAndThen(updateComponents: Seq[ReachComponent])(f: => Unit): Unit
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return Whether this component uses custom cursor painting features
	  */
	def isManagingCursor = cursorManager.nonEmpty
	
	
	// OTHER	------------------------------
	
	/**
	  * Performs a layout update for all components in this canvas. Should be called when setting up this canvas /
	  * if updates were previously ignored and can't be tracked anymore
	  * @param targetContentSize Size to assign for the managed component
	  */
	def updateWholeLayout(targetContentSize: Size) =
		currentContent.foreach { content =>
			updateLayout(content.toTree.branchesIterator.map { _.map { _.nav } }.toSet, targetContentSize)
		}
	
	/**
	  * Updates component layout based on queued updates
	  * @param queues Sequences of components from hierarchy top downwards that require a layout update
	  * @param componentTargetSize Size to assign for the managed component
	  */
	protected def updateLayout(queues: Set[Seq[ReachComponent]], componentTargetSize: Size) = {
		// Updates content size
		val contentSizeChanged = currentContent match {
			case Some(content) =>
				val requiresSizeUpdate = content.size != componentTargetSize
				if (requiresSizeUpdate)
					content.size = componentTargetSize
				requiresSizeUpdate
			case None => false
		}
		// Updates content layout
		val layoutUpdateQueues = queues.map { q: Seq[ReachComponent] => q -> contentSizeChanged }
		val sizeChangeTargets: Set[ReachComponent] = {
			if (contentSizeChanged)
				currentContent.toSet
			else
				Set()
		}
		if (layoutUpdateQueues.nonEmpty || sizeChangeTargets.nonEmpty) {
			updateLayoutFor(layoutUpdateQueues, sizeChangeTargets).foreach { repaint(_) }
		}
	}
	
	/**
	  * Requests a repaint for this whole canvas element
	  */
	def repaint() = currentPainter.foreach { _.repaintAll() }
	
	/**
	  * Repaints a part of this canvas
	  * @param area Area to paint again
	  * @param priority Priority to use for this repaint. The high level priority areas are painted first.
	  */
	def repaint(area: Bounds, priority: Priority = Priority.Normal) =
		currentPainter.foreach { _.repaintRegion(area, priority) }
	
	/**
	  * Shifts a painted region inside these canvases
	  * @param originalArea The area to shift (relative to this canvas' top left corner)
	  * @param translation Translation vector to apply to the area
	  */
	def shiftArea(originalArea: Bounds, translation: Vector2D) =
		currentPainter.foreach { _.shift(originalArea, translation) }
	
	// Second parameter in queues is whether a repaint operation has already been queued for them
	// Resized children are expected to have their repaints already queued
	// Returns areas to repaint afterwards
	private def updateLayoutFor(componentQueues: Set[(Seq[ReachComponent], Boolean)],
	                            sizeChangedChildren: Set[ReachComponent]): Vector[Bounds] =
	{
		val nextSizeChangeChildrenBuilder = new VectorBuilder[ReachComponent]()
		val nextPositionChangeChildrenBuilder = new VectorBuilder[ReachComponent]()
		val repaintZonesBuilder = new VectorBuilder[Bounds]()
		
		// Component -> Whether paint operation has already been queued
		val nextTargets = componentQueues
			.map { case (queue, wasPainted) => queue.head -> wasPainted } ++ sizeChangedChildren.map { _ -> true }
		// Updates the layout of the next layer (from top to bottom) components.
		// Checks for size (and possible position) changes and queues updates for the children of components which
		// changed size during the layout update
		// Also, collects any repaint requirements
		nextTargets.foreach { case (component, wasPainted) =>
			// Caches bounds before update
			val oldChildBounds = component.children.map { c => c -> c.bounds }
			// Applies component update
			component.updateLayout()
			// Queues child updates (on size changes) and possible repaints
			// (only in components where no repaint has occurred yet)
			if (wasPainted)
				oldChildBounds.foreach { case (child, oldBounds) =>
					if (child.size != oldBounds.size)
						nextSizeChangeChildrenBuilder += child
				}
			else
				oldChildBounds.foreach { case (child, oldBounds) =>
					val currentBounds = child.bounds
					if (currentBounds != oldBounds) {
						repaintZonesBuilder += (Bounds.around(Pair(oldBounds, currentBounds)) +
							child.hierarchy.positionToTopModifier)
						if (oldBounds.size != currentBounds.size)
							nextSizeChangeChildrenBuilder += child
						else
							nextPositionChangeChildrenBuilder += child
					}
				}
		}
		
		// Moves to the next layer of components, if there is one
		val nextSizeChangedChildren = nextSizeChangeChildrenBuilder.result().toSet
		val paintedChildren = nextSizeChangedChildren ++ nextPositionChangeChildrenBuilder.result()
		val (leaves, branches) = componentQueues.divideBy { _._1.hasSize > 1 }.toTuple
		val nextQueues = branches.map { case (queue, wasPainted) =>
			if (wasPainted)
				queue.tail -> wasPainted
			else
				// Checks whether a paint operation was queued for this component already
				queue.tail -> paintedChildren.contains(queue(1))
		}
		// Also, paints all of the lowest revalidation levels, unless their parents were already painted
		leaves.iterator.foreach { case (queue, wasPainted) =>
			if (!wasPainted && !paintedChildren.contains(queue.head))
				repaintZonesBuilder += queue.head.boundsInsideTop
		}
		val repaintZones = repaintZonesBuilder.result()
		// Case: Reached the bottom => Returns
		if (nextQueues.isEmpty && nextSizeChangedChildren.isEmpty)
			repaintZones
		// Case: There are more branches / depth => Moves one level deeper
		else
			 repaintZones ++ updateLayoutFor(nextQueues, nextSizeChangedChildren)
	}
}
