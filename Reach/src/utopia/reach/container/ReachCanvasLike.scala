package utopia.reach.container

import utopia.genesis.graphics.{PaintManager, Priority}
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.ReachComponent
import utopia.reach.cursor.ReachCursorManager
import utopia.reach.focus.ReachFocusManager

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
	def updateWholeLayout(targetContentSize: Size) = currentContent.foreach { content =>
		content.size = targetContentSize
		content.updateWholeLayout()
		repaint()
	}
	
	/**
	  * Updates component layout based on queued updates
	  * @param queues Sequences of components from hierarchy top downwards that require a layout update
	  * @param componentTargetSize Size to assign for the managed component
	  */
	protected def updateLayout(queues: Set[Seq[ReachComponent]], componentTargetSize: Size) =
		currentContent.foreach { content =>
			// Updates the content size, if appropriate
			val requiresSizeUpdate = content.size != componentTargetSize
			if (requiresSizeUpdate)
				content.size = componentTargetSize
				
			// Performs the layout updates throughout the component hierarchy
			ComponentHierarchy.updateLayoutFor(content, queues, topChangedSize = requiresSizeUpdate, topIsCanvas = true)
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
}
