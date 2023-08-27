package utopia.reach.dnd

import utopia.flow.event.model.ChangeEvent
import utopia.paradigm.shape.shape2d.vector.point.Point

import java.nio.file.Path

/**
  * Common trait for file drag-and-drop events.
  * These events occur when the user drags a file or files into the software.
  * These events may be general (i.e. same for all listeners), or relative (specific to a single listener)
  * @author Mikko Hilpinen
  * @since 19.2.2023, v0.5.1
  */
sealed trait DragAndDropEvent
{
	/**
	  * @return Whether this is the final event in a drag-and-drop process.
	  *         The drag-and-drop process is considered to end when the user drops the file/files,
	  *         or when they drag the files back away from the parent canvas without dropping them.
	  */
	def isFinal: Boolean
}

/**
  * Common trait for relative file drag-and-drop events.
  * Relative events are different for different listeners / target components,
  * because they are affected by the component's bounds.
  */
sealed trait RelativeDragAndDropEvent extends DragAndDropEvent
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The current mouse coordinate relative to the target component's top-left corner (i.e. position)
	  */
	def relativePosition: Point
	/**
	  * @return The current mouse coordinate relative to the canvas' top-left corner.
	  */
	def positionInCanvas: Point
	
	/**
	  * @return Whether the mouse / file is currently over the target component's bounds
	  */
	def isOver: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Whether the mouse / file is currently outside of the target component's bounds
	  */
	def isOutside = !isOver
}

/**
  * Common trait for relative file-dragging events, where the mouse/file location changes
  */
sealed trait RelativeDragEvent extends RelativeDragAndDropEvent
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The drag location change in relative component space.
	  *         I.e. relative to the component's position.
	  */
	def relativeChange: ChangeEvent[Point]
	/**
	  * @return The drag location change in canvas coordinate system.
	  *         I.e. relative to the top-left corner of the canvas component.
	  */
	def canvasChange: ChangeEvent[Point]
	
	/**
	  * @return Whether the mouse / file just entered the target component's bounds
	  */
	def entered: Boolean
	/**
	  * @return Whether the mouse / file just exited the target component's bounds
	  */
	def exited: Boolean
	
	
	// IMPLEMENTED -----------------------
	
	override def isFinal: Boolean = false
	
	override def relativePosition = relativeChange.newValue
	override def positionInCanvas = canvasChange.newValue
}

object DragAndDropEvent
{
	/**
	  * Fired when the user drags a file into the canvas component
	  */
	case object EnterCanvas extends DragAndDropEvent
	{
		override def isFinal: Boolean = false
	}
	/**
	  * Fired when the user drags a file from the canvas component to a location outside the canvas
	  */
	case object ExitCanvas extends DragAndDropEvent
	{
		override def isFinal: Boolean = true
	}
	
	/**
	  * Fired when a file is dropped to another component, terminating the drag-and-drop process.
	  */
	case object DropToOther extends DragAndDropEvent
	{
		override def isFinal: Boolean = true
	}
	
	/**
	  * Fired when the user drags a file into the target component
	  * @param relativeChange Change in mouse position, relative to the target component's top-left corner
	  * @param canvasChange Change in mouse position, relative to the canvas' top-left corner
	  */
	case class Enter(relativeChange: ChangeEvent[Point], canvasChange: ChangeEvent[Point])
		extends RelativeDragEvent
	{
		override def entered: Boolean = true
		override def exited: Boolean = false
		override def isOver: Boolean = true
	}
	/**
	  * Fired when the user moves a file over the target component
	  * @param relativeChange Change in mouse position, relative to the target component's top-left corner
	  * @param canvasChange   Change in mouse position, relative to the canvas' top-left corner
	  */
	case class Over(relativeChange: ChangeEvent[Point], canvasChange: ChangeEvent[Point])
		extends RelativeDragEvent
	{
		override def entered: Boolean = false
		override def exited: Boolean = false
		override def isOver: Boolean = true
	}
	/**
	  * Fired when the user drags a file into a location outside the target component
	  * @param relativeChange Change in mouse position, relative to the target component's top-left corner
	  * @param canvasChange   Change in mouse position, relative to the canvas' top-left corner
	  */
	case class Exit(relativeChange: ChangeEvent[Point], canvasChange: ChangeEvent[Point])
		extends RelativeDragEvent
	{
		override def entered: Boolean = false
		override def exited: Boolean = true
		override def isOver: Boolean = false
	}
	/**
	  * Fired when the user drags a file in area outside the target component, but within the canvas component
	  * @param relativeChange Change in mouse position, relative to the target component's top-left corner
	  * @param canvasChange   Change in mouse position, relative to the canvas' top-left corner
	  */
	case class Outside(relativeChange: ChangeEvent[Point], canvasChange: ChangeEvent[Point])
		extends RelativeDragEvent
	{
		override def entered: Boolean = false
		override def exited: Boolean = false
		override def isOver: Boolean = false
	}
	
	/**
	  * Fired when the user drops a file into an area within the target component,
	  * terminating the drag-and-drop process.
	  * @param relativePosition Position where the file was dropped, relative to the target component's top-left corner
	  * @param positionInCanvas Position where the file was dropped,
	  *                         relative to the top-left corner of the canvas component
	  * @param files The files that were dropped
	  */
	case class Drop(relativePosition: Point, positionInCanvas: Point, files: Vector[Path])
		extends RelativeDragAndDropEvent
	{
		override def isFinal: Boolean = true
		override def isOver: Boolean = true
	}
}