package utopia.genesis.event

import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.util.Filter
import utopia.paradigm.shape.shape2d.{Point, RelativePoint}

object MouseDragEvent
{
	/**
	 * A filter that only accepts drag end events
	 */
	lazy val dragEndFilter = filter { _.isDragEnd }
	/**
	 * A filter that only accepts drags performed with the left mouse button
	 */
	lazy val leftButtonFilter = buttonFilter(MouseButton.Left)
	/**
	 * A filter that only accepts drags performed with the right mouse button
	 */
	lazy val rightButtonFilter = buttonFilter(MouseButton.Right)
	
	/**
	 * @param f A filter function
	 * @return A mouse drag event filter based on that function
	 */
	def filter(f: MouseDragEvent => Boolean) = Filter(f)
	
	/**
	 * @param movementFilter A filter applicable for mouse movement events
	 * @return That filter applying to mouse drag events
	 */
	def filterByMovement(movementFilter: Filter[MouseMoveEvent]) =
		filter { e => movementFilter(e.lastMove) }
	/**
	 * @param keyStatusFilter A filter applicable to keyboard states
	 * @return That filter applying to mouse drags
	 */
	def filterByKeys(keyStatusFilter: Filter[KeyStatus]) =
		filter { e => keyStatusFilter(e.startKeyStatus) }
	
	/**
	 * @param button Required mouse button
	 * @return A filter that only accepts drags performed using that mouse button
	 */
	def buttonFilter(button: MouseButton) = filter { _.button == button }
}

/**
 * Common trait for different mouse drag events
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2
 * @param dragOrigin The position where the drag started
 * @param lastMove The latest mouse movement event
 * @param button The mouse button used in this drag
 * @param startKeyStatus Keyboard status at drag start
 * @param isDown Whether the mouse button is currently down (false at the end of the drag, otherwise true)
 */
case class MouseDragEvent(dragOrigin: RelativePoint, lastMove: MouseMoveEvent, button: MouseButton,
                          startKeyStatus: KeyStatus = GlobalKeyboardEventHandler.keyStatus, isDown: Boolean = true)
	extends MouseEvent[MouseDragEvent]
{
	// COMPUTED --------------------------------
	
	/**
	 * @return Whether this event represents the end of a drag
	 */
	def isDragEnd = !isDown
	/**
	 * @return Whether this event represents the start of a drag
	 */
	def isDragStart = isDown && dragOrigin.relative == lastMove.previousMousePosition
	
	/**
	 * @return The total (linear) movement occurred during this drag
	 */
	def totalMovement = mousePosition.toVector - dragOrigin
	
	
	// IMPLEMENTED  ----------------------------
	
	override def mousePosition: Point = lastMove.mousePosition
	override def absoluteMousePosition: Point = lastMove.absoluteMousePosition
	
	override def buttonStatus: MouseButtonStatus = lastMove.buttonStatus
	
	// NB: Only maps relative, because that's how other mouse events work at this time
	override def mapPosition(f: Point => Point): MouseDragEvent =
		copy(dragOrigin.mapRelative(f), lastMove.mapPosition(f))
}
