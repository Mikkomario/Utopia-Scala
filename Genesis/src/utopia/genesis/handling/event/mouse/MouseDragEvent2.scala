package utopia.genesis.handling.event.mouse

import utopia.flow.collection.immutable.Pair
import utopia.genesis.event.MouseButton
import utopia.genesis.handling.event.keyboard.KeyboardState
import utopia.genesis.handling.event.mouse.MouseDragListener2.MouseDragEventFilter
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.FiniteDuration

object MouseDragEvent2
{
	/**
	  * @return Access points to mouse drag -event filters
	  */
	def filter = MouseDragEventFilter
}

/**
 * An event fired when the mouse is moved from one location to another, while holding a mouse button down
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2
 * @param dragOrigin The position where the drag started
 * @param lastMove The latest mouse movement event
 * @param button The mouse button used in this drag
 * @param startKeyboardState Keyboard state at drag start
 * @param pressed Whether the associated mouse button is still pressed.
  *                False if the button was released, which also signals the end of the drag.
 */
case class MouseDragEvent2(dragOrigin: RelativePoint, lastMove: MouseMoveEvent2, override val button: MouseButton,
                           startKeyboardState: KeyboardState, override val pressed: Boolean = true)
	extends MouseMoveEventLike[MouseDragEvent2] with MouseButtonStateEventLike[MouseDragEvent2]
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * @return The total (linear) movement occurred during this drag (i.e. not just during this event)
	  */
	lazy val totalMovement = position.toVector - dragOrigin
	
	
	// COMPUTED --------------------------------
	
	/**
	 * @return Whether this event represents the end of a drag
	 */
	def isDragEnd = !pressed
	/**
	 * @return Whether this event represents the start of a drag
	 */
	def isDragStart = pressed && dragOrigin.relative == lastMove.previousPosition.relative
	
	@deprecated("Please use .startKeyboardState instead", "v4.0")
	def startKeyStatus = startKeyboardState
	
	
	// IMPLEMENTED  ----------------------------
	
	override def positions: Pair[RelativePoint] = lastMove.positions
	override def duration: FiniteDuration = lastMove.duration
	override def buttonStates: MouseButtonStates = lastMove.buttonStates
	
	override def withPositions(positions: Pair[RelativePoint]): MouseDragEvent2 =
		copy(lastMove = lastMove.withPositions(positions))
}
