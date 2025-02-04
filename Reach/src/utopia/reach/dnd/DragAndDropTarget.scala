package utopia.reach.dnd

import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.HasSize
import utopia.reach.component.template.ReachComponent

object DragAndDropTarget
{
	// OTHER    ---------------------------
	
	/**
	  * Wraps a component as a drag-and-drop target
	  * @param component The component to wrap
	  * @param acceptNearbyDrops Whether nearby file drops should be converted to local (over) drops (default = false)
	  * @param f A function which processes drag-and-drop events
	  * @tparam U Arbitrary function return type
	  * @return A new drag-and-drop target that function's over the component's area
	  */
	def wrap[U](component: ReachComponent, acceptNearbyDrops: Boolean = false)
	            (f: DragAndDropEvent => U): DragAndDropTarget =
		new _DragAndDropTarget[U](component.boundsInsideTop, f, (_, _) => acceptNearbyDrops)
	
	/**
	  * Creates a drag-and-drop target that functions over the whole canvas element
	  * @param canvas A canvas element
	  * @param f A function that receives drag-and-drop events
	  * @tparam U Arbitrary function return type
	  * @return A new drag-and-drop target
	  */
	def anywhereInCanvas[U](canvas: HasSize)(f: DragAndDropEvent => U): DragAndDropTarget =
		new _DragAndDropTarget(Bounds(Point.origin, canvas.size), f)
	
	
	// NESTED   ---------------------------
	
	private class _DragAndDropTarget[U](area: => Bounds, onEvent: DragAndDropEvent => U,
	                                    onNearby: (Point, Point) => Boolean = (_, _) => false)
		extends DragAndDropTarget
	{
		override def dropArea: Bounds = area
		
		override def onDragAndDropEvent(event: DragAndDropEvent): Unit = onEvent(event)
		
		override def onDropNearby(relativeDropPoint: Point, dropPointInCanvas: Point): Boolean =
			onNearby(relativeDropPoint, dropPointInCanvas)
	}
}

/**
  * Common trait for components or other items that receive file drops within a specific area
  * @author Mikko Hilpinen
  * @since 19.2.2023, v0.5.1
  */
trait DragAndDropTarget
{
	/**
	  * @return The bounds of the area where file drops are accepted.
	  *         Relative to the top-left corner of the parent ReachCanvas component.
	  */
	def dropArea: Bounds
	
	/**
	  * This function is called whenever a new drag-and-drop event occurs
	  * @param event The event that occurred
	  */
	def onDragAndDropEvent(event: DragAndDropEvent): Unit
	
	/**
	  * This function is called when a file is dropped to a location outside of any drag-and-drop targets.
	  * The targets may respond to this function call in order to receive the drop to themselves.
	  * @param relativeDropPoint Point where the file was dropped, relative to this target's top-left-corner
	  * @param dropPointInCanvas Point where the file was dropped, relative to the canvas top-left corner
	  * @return Whether a Drop event should be generated for this listener, based on this event.
	  *         If false, a DropToOther -event will be generated instead.
	  */
	def onDropNearby(relativeDropPoint: Point, dropPointInCanvas: Point): Boolean
}
