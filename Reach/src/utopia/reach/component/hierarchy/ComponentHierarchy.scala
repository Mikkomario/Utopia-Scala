package utopia.reach.component.hierarchy

import utopia.firmament.model.CoordinateTransform
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.util.EitherExtensions._
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.graphics.Priority.Normal
import utopia.genesis.graphics.{FontMetricsWrapper, Priority}
import utopia.genesis.text.Font
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.reach.component.template.ReachComponent
import utopia.reach.container.ReachCanvas

import scala.annotation.tailrec
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

object ComponentHierarchy
{
	/**
	 * Updates component layout based on queued updates
	 * @param top The top component in the component hierarchy, if applicable.
	 * @param queues Sequences of components from hierarchy top downwards that require a layout update
	 * @param topChangedSize Whether the 'top' component changed its size before this layout update (default = false)
	 * @param topIsCanvas Whether the 'top' component represents the Reach Canvas.
	 *                    Affects repainting and component position-detection logic, for example.
	 *                    Default = false.
	 */
	def updateLayoutFor(top: ReachComponent, queues: Iterable[Seq[ReachComponent]], topChangedSize: Boolean = false,
	                    topIsCanvas: Boolean = false): Unit =
	{
		// Updates content layout
		val layoutUpdateQueues = queues.view.map { q => (q, topChangedSize, q.size) }.toOptimizedSeq
		val sizeChangeTargets: Set[ReachComponent] = if (topChangedSize) Set(top) else Set()
		if (layoutUpdateQueues.nonEmpty || sizeChangeTargets.nonEmpty) {
			// Performs the layout updates. The repaint logic is different for the root level component.
			if (topIsCanvas) {
				val boundsBuilder = OptimizedIndexedSeq.newBuilder[Bounds]
				updateLayoutFor(if (topIsCanvas) None else Some(top), layoutUpdateQueues, sizeChangeTargets,
					boundsBuilder, 0)
				lazy val canvas = top.hierarchy.top
				boundsBuilder.result().foreach { canvas.repaint(_) }
			}
			else {
				val boundsBuilder = Bounds.aroundBuilder
				updateLayoutFor(if (topIsCanvas) None else Some(top), layoutUpdateQueues, sizeChangeTargets,
					boundsBuilder, 0)
				boundsBuilder.resultOption().foreach { top.repaintArea(_) }
			}
		}
	}
	
	/**
	 * Updates the layout of the specified components. Queues repaints as appropriate.
	 * @param topComponent The component relative to which the repaint requests are made
	 * @param componentQueues The component queues that are yet to be fully updated.
	 *                        Each element contains 3 values:
	 *                              1. The component queue (from top to bottom)
	 *                              1. Whether a repaint has already been queued higher up
	 *                              1. The length of this queue
	 * @param sizeChangedChildren Collection of components that had their size altered during the previous iteration.
	 *                            These are all updated as well.
	 * @param repaintZonesBuilder A builder for the resulting repaint areas
	 * @param queueIndex Next queue index to process
	 */
	@tailrec
	private def updateLayoutFor(topComponent: Option[ReachComponent],
	                            componentQueues: Iterable[(Seq[ReachComponent], Boolean, Int)],
	                            sizeChangedChildren: collection.Set[ReachComponent],
	                            repaintZonesBuilder: mutable.Growable[Bounds], queueIndex: Int): Unit =
	{
		// Records the components that had their size or position changed,
		// as well as new areas that need repainting afterwards
		val nextSizeChangeChildrenBuilder = mutable.Set[ReachComponent]()
		val nextPositionChangeChildrenBuilder = mutable.Set[ReachComponent]()
		
		// Determines the next components for which layout is updated.
		// I.e. next queued components, as well as the latest components that had their size adjusted.
		//      Component -> Whether paint operation has already been queued
		val nextTargetsView =
			componentQueues.view.map { case (queue, wasPainted, _) => queue(queueIndex) -> wasPainted } ++
				sizeChangedChildren.view.map { _ -> true }
		// Updates the layout of the next layer (from top to bottom) components.
		// Checks for size (and possible position) changes and queues updates for the children of components which
		// changed size during the layout update
		// Also, collects any repaint requirements
		nextTargetsView.foreach { case (component, wasPainted) =>
			// Caches bounds before update
			val oldChildBounds = component.children.map { c => c -> c.bounds }
			// Applies component update
			component.updateLayout()
			// Queues child updates (on size changes) and possible repaints
			// (only in components where no repaint has occurred yet)
			
			// Case: This component was already queued for repaint
			//       => Only checks if any of the child components changed size
			if (wasPainted)
				nextSizeChangeChildrenBuilder ++=
					oldChildBounds.iterator.filter { case (child, oldBounds) => child.size != oldBounds.size }
						.map { _._1 }
			// Case: This component was not queued for repaint
			//       => Checks if any of the child component bounds were modified and queues repaints, as necessary
			else
				oldChildBounds.foreach { case (child, oldBounds) =>
					val currentBounds = child.bounds
					if (currentBounds != oldBounds) {
						// Queues a repaint (in the top component's coordinate space)
						repaintZonesBuilder += (Bounds.around(Pair(oldBounds, currentBounds)) +
							topComponent.flatMap(child.hierarchy.positionInComponentModifier)
								.getOrElse { child.hierarchy.positionToTopModifier })
						
						// Case: Size changed
						//       => Remembers this component, so that it will be updated on the next iteration
						if (oldBounds.size != currentBounds.size)
							nextSizeChangeChildrenBuilder += child
						// Case: Only position changed => Remembers that a repaint was queued for this component
						else
							nextPositionChangeChildrenBuilder += child
					}
				}
		}
		
		// Moves to the next layer of components, if there is one
		// Checks which of the queues contain more components to handle
		val paintedChildren = Set.concat(nextSizeChangeChildrenBuilder, nextPositionChangeChildrenBuilder)
		val nextQueueIndex = queueIndex + 1
		val (leaves, remainingQueues) = componentQueues.divideWith { case (queue, wasPainted, length) =>
			// Case: Queue contains more components
			//       => Updates the painted status and prepares the queue for the next iteration
			if (length > nextQueueIndex)
				Right((queue, wasPainted || paintedChildren.contains(queue(nextQueueIndex)), length))
			// Case: Queue ended here => Collects the last component for repainting, if appropriate
			else {
				val leaf = queue(queueIndex)
				Left((leaf, wasPainted || paintedChildren.contains(leaf)))
			}
		}
		// Paints all the lowest revalidation levels, unless their parents were already painted
		repaintZonesBuilder ++= leaves.iterator.filterNot { _._2 }.map { case (leaf, _) =>
			topComponent.flatMap(leaf.boundsInside).getOrElse(leaf.boundsInsideTop)
		}
		
		// Checks whether more iterations are appropriate
		// Case: More components to update => Continues using recursion
		if (remainingQueues.nonEmpty || nextSizeChangeChildrenBuilder.nonEmpty) {
			updateLayoutFor(topComponent, remainingQueues, nextSizeChangeChildrenBuilder, repaintZonesBuilder,
				nextQueueIndex)
		}
	}
}

/**
  * Represents a sequence of components that forms a linear hierarchy
  * @author Mikko Hilpinen
  * @since 3.10.2020, v0.1
  */
// TODO: For the revalidation implementation, may add a pointer for delaying repaints
trait ComponentHierarchy
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The next "block" in this hierarchy (either Left: Canvas at the top or
	  *         Right: an intermediate block + a component associated with that block)
	  */
	def parent: Either[ReachCanvas, (ComponentHierarchy, ReachComponent)]
	
	/**
	  * @return A pointer that shows whether this hierarchy is currently active / linked to the top window.
	  *         Should take into account possible parent state.
	  */
	def linkedFlag: Flag
	// TODO: Consider adding the following two methods
	/*
	  * @return A pointer that contains the direct parent component's position relative to the root canvas component
	  */
	// def parentPositionInCanvasPointer: Changing[Point]
	/*
	  * @return A pointer that contains this component's position on the screen
	  */
	// def absoluteParentPositionPointer: Changing[Point]
	
	/**
	  * @return Whether the link between this component and the parent component should be considered active
	  */
	def isThisLevelLinked: Boolean
	
	/**
	  * @return Coordinate transform converting parent coordinates to relative, local coordinates.
	  *         None if no coordinate transform is necessary.
	  */
	def coordinateTransform: Option[CoordinateTransform]
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return The component directly over this component
	  */
	def parentComponent = parent.leftOrMap { _._2 }
	/**
	  * @return An iterator that returns components from this level's parents upward.
	  *         Will stop at (i.e. not include) the ReachCanvas instance.
	  */
	def parentsIterator =
		OptionsIterator.iterate(parent.toOption) { _._1.parent.toOption }.map { _._2 }
	/**
	  * @return The canvas at the top of this hierarchy
	  */
	def top: ReachCanvas = parent match {
		case Left(canvas) => canvas
		case Right((block, _)) => block.top
	}
	
	/**
	  * @return Whether this hierarchy currently reaches the top component without any broken links
	  */
	def isLinked = linkedFlag.value
	/**
	  * @return Whether this hierarchy doesn't reach the top component at this time
	  */
	def isDetached = !isLinked
	
	/**
	  * @return The window that contains this component hierarchy. None if not connected to a window.
	  */
	def parentWindow = top.parentWindow
	
	/**
	  * @return A modifier used when calculating the position of the bottom component (outside of this hierarchy)
	  *         relative to hierarchy top
	  */
	def positionToTopModifier: Vector2D = transform(defaultPositionToTopModifier)
	/**
	  * @return A modifier used for calculating the absolute position of the bottom component (not on this hierarchy)
	  */
	def absolutePositionModifier = transform(defaultPositionToTopModifier + top.absolutePosition)
	
	/**
	  * @return A linear component sequence based on this component hierarchy. The higher hierarchy components are
	  *         placed in the beginning and the last element is the first direct parent component. If this hierarchy
	  *         doesn't have parents before the canvas, returns an empty vector.
	  */
	@deprecated("Please use .toSeq instead", "v1.6.1")
	def toVector: Vector[ReachComponent] = parentsIterator.toVector.reverse
	/**
	  * @return A linear component sequence based on this component hierarchy. The higher hierarchy components are
	  *         placed in the beginning and the last element is the first direct parent component. If this hierarchy
	  *         doesn't have parents before the canvas, returns an empty sequence.
	  */
	def toSeq: Seq[ReachComponent] = OptimizedIndexedSeq.from(parentsIterator).reverse
	
	@deprecated("Deprecated for removal. Please use .linkedFlag instead", "v1.6")
	def linkPointer: Flag = linkedFlag
	
	private def defaultPositionToTopModifier = parent match {
		case Left(_) => Vector2D.zero
		case Right((block, component)) => block.positionToTopModifier + component.position
	}
	
	
	// OTHER	--------------------------
	
	/**
	  * @param hierarchy A component hierarchy
	  * @return Whether this component hierarchy block is under specified component hierarchy
	  */
	@tailrec
	final def isChildOf(hierarchy: ComponentHierarchy): Boolean = parent match {
		case Right((parentHierarchy, _)) => parentHierarchy == hierarchy || parentHierarchy.isChildOf(hierarchy)
		case Left(_) => false
	}
	/**
	  * @param component A component
	  * @return Whether this component hierarchy block is under specified component
	  */
	@tailrec
	final def isChildOf(component: ReachComponent): Boolean = parent match {
		case Right((parentHierarchy, parentComponent)) =>
			parentComponent == component || parentHierarchy.isChildOf(component)
		case Left(_) => false
	}
	/**
	  * @param component A component
	  * @return Whether that component is part of this hierarchy (either below or above)
	  */
	def contains(component: ReachComponent) =
		component.hierarchy == this || component.isChildOf(this) || isChildOf(component)
	
	/**
	  * @param component A component
	  * @return A modifier to apply to this hierarchy's child's position in order to get the position in the specified
	  *         component. None if this hierarchy is not a child of the specified component.
	  */
	def positionInComponentModifier(component: ReachComponent): Option[Vector2D] = parent match {
		case Right((parentHierarchy, parentComponent)) =>
			val default = {
				if (parentComponent == component)
					Some(Vector2D.zero)
				else
					parentHierarchy.positionInComponentModifier(component).map { _ + parentComponent.position }
			}
			default.map(transform)
		case Left(_) => None
	}
	
	/**
	  * Revalidates this component hierarchy (provided this part of the hierarchy is currently linked to the main
	  * stack hierarchy)
	  */
	def revalidate(layoutUpdateComponents: Seq[ReachComponent]): Unit = {
		val branchBuilder = new VectorBuilder[ReachComponent]()
		layoutUpdateComponents.reverseIterator.foreach { branchBuilder += _ }
		_revalidate(branchBuilder) { _.foreach { case (canvas, queue) => canvas.revalidate(queue) } }
	}
	/**
	  * Revalidates this component hierarchy (provided this part of the hierarchy is currently linked to the main
	  * stack hierarchy), then calls the specified function
	  * @param f A function called once this hierarchy has been updated. Please note that this function might not
	  *          get called at all.
	  */
	def revalidateAndThen(layoutUpdateComponents: Seq[ReachComponent])(f: => Unit): Unit = {
		val branchBuilder = new VectorBuilder[ReachComponent]()
		layoutUpdateComponents.reverseIterator.foreach { branchBuilder += _ }
		_revalidate(branchBuilder) {
			case Some((canvas, queue)) => canvas.revalidateAndThen(queue)(f)
			case None => f
		}
	}
	@tailrec
	private def _revalidate(branchBuilder: mutable.Builder[ReachComponent, Seq[ReachComponent]])
	                       (atTop: Option[(ReachCanvas, Seq[ReachComponent])] => Unit): Unit =
	{
		// Terminates if not linked
		if (isThisLevelLinked)
			parent match {
				// Case: Top reached => Performs the revalidation by calling the canvas
				case Left(canvas) => atTop(Some(canvas -> branchBuilder.result()))
				// Case: Parent is not a canvas => Resets its stack size
				case Right((block, component)) =>
					branchBuilder += component
					// Case: Stack size may have changed => Propagates the revalidation further up
					if (component.updateStackSize())
						block._revalidate(branchBuilder)(atTop)
					// Case: Stack size didn't change => Performs the layout updates & repainting on a local level
					else {
						ComponentHierarchy.updateLayoutFor(component, Single(branchBuilder.result()))
						atTop(None)
					}
			}
	}
	
	/**
	  * Repaints the whole component hierarchy (if linked)
	  */
	def repaintAll() = if (isLinked) top.repaint()
	/**
	  * Repaints a sub-section of the bottom component (if linked to top)
	  * @param area Area inside the bottom component
	  * @param priority Priority used for this repaint operation. Higher priority areas are painted first
	  *                 (default = Normal)
	  */
	def repaint(area: => Bounds, priority: Priority = Normal) = {
		if (isLinked) {
			val areaInTop = inverseTransform(area) + defaultPositionToTopModifier
			top.repaint(areaInTop, priority)
		}
	}
	/**
	  * Repaints the bottom component
	  */
	def repaintBottom(priority: Priority = Normal) = {
		if (isLinked)
			parent match {
				case Left(canvas) => canvas.repaint()
				case Right((block, component)) => block.repaint(component.bounds, priority)
			}
	}
	
	/**
	  * Shifts a painted area by specified amount
	  * @param area A region in this component hierarchy (lowest parent's position system) to shift
	  * @param translation The amount of translation applied to the area
	  */
	def shiftArea(area: => Bounds, translation: => Vector2D) = {
		if (isLinked)
			top.shiftArea(inverseTransform(area) + defaultPositionToTopModifier, translation)
	}
	
	/**
	  * @param font Font to use
	  * @return Metrics for that font
	  */
	// TODO: Refactor this once component class hierarchy has been updated
	def fontMetricsWith(font: Font): FontMetricsWrapper = top.component.getFontMetrics(font.toAwt)
	
	// Transforms a coordinate from parent space to the relative space under this hierarchy
	private def transform[V <: DoubleVectorLike[V]](coordinate: V) = coordinateTransform match {
		case Some(transform) => transform(coordinate)
		case None => coordinate
	}
	// Transforms a relative area to the parent's coordinate space
	private def inverseTransform(area: Bounds) = coordinateTransform.map { _.invert(area) } match {
		case Some(transformed) => transformed.bounds
		case None => area
	}
}
