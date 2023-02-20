package utopia.genesis.view

import utopia.flow.view.mutable.Pointer
import utopia.genesis.event.{ConsumeEvent, KeyStatus, MouseButton, MouseButtonStateEvent, MouseDragEvent, MouseMoveEvent}
import utopia.genesis.handling.{MouseButtonStateListener, MouseDragListener, MouseMoveListener}
import utopia.inception.handling.HandlerType
import utopia.paradigm.shape.shape2d.RelativePoint

/**
 * Used for generating mouse drag events based on mouse button and mouse move events.
 * Remember to add this tracker to appropriate handlers.
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2.1
 */
class DragTracker(listener: MouseDragListener) extends MouseButtonStateListener with MouseMoveListener
{
	// ATTRIBUTES   --------------------
	
	private val dragPointers = MouseButton.values
		.map { b => b -> Pointer.empty[(DragStart, Option[MouseMoveEvent])]() }.toMap
	
	
	// IMPLEMENTED  --------------------
	
	override def onMouseButtonState(event: MouseButtonStateEvent): Option[ConsumeEvent] =
	{
		event.button.foreach { button =>
			val pointer = dragPointers(button)
			// Case: Mouse button pressed => Prepares a drag
			if (event.isDown) {
				val p = RelativePoint(event.mousePosition, event.absoluteMousePosition)
				pointer.value = Some(DragStart(p), None)
			}
			// Case: Mouse button release => Generates a drag end event, if there was movement
			else
				pointer.pop().foreach { case (start, lastMove) =>
					lastMove.foreach { move =>
						val event = MouseDragEvent(start.position, move, button, start.keyState, isDown = false)
						listener.onMouseDrag(event)
					}
				}
		}
		None
	}
	
	// Updates mouse positions and generates drag events
	override def onMouseMove(event: MouseMoveEvent): Unit =
	{
		val events = dragPointers.flatMap { case (button, pointer) =>
			pointer.updateAndGet { _.map { case (start, _) => start -> Some(event) } }
				.map { case (start, _) => MouseDragEvent(start.position, event, button, start.keyState) }
		}
		events.foreach(listener.onMouseDrag)
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType): Boolean = true
	
	
	// NESTED   ------------------------
	
	private case class DragStart(position: RelativePoint,
	                             keyState: KeyStatus = GlobalKeyboardEventHandler.keyStatus)
}
