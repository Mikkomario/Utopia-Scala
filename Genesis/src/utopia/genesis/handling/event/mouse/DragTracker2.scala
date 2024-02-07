package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.event.MouseButton
import utopia.genesis.handling.event.ConsumeChoice
import utopia.genesis.handling.event.keyboard.{KeyboardEvents, KeyboardState}
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

/**
 * Used for generating mouse drag events based on mouse button and mouse move events.
 * Remember to add this tracker to appropriate handlers.
 * @author Mikko Hilpinen
 * @since 20.2.2023, v3.2.1
 */
class DragTracker2(listener: MouseDragListener2) extends MouseButtonStateListener2 with MouseMoveListener2
{
	// ATTRIBUTES   --------------------
	
	private val dragPointers = MouseButton.standardValues
		.map { b => b -> Pointer.empty[(DragStart, Option[MouseMoveEvent2])]() }.toMap
	
	
	// IMPLEMENTED  --------------------
	
	override def handleCondition: FlagLike = AlwaysTrue
	
	override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2] = AcceptAll
	override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice = {
		dragPointers.get(event.button).foreach { pointer =>
			// Case: Mouse button pressed => Prepares a drag
			if (event.pressed)
				pointer.value = Some(DragStart(event.position), None)
			// Case: Mouse button release => Generates a drag end event, if there was movement
			else
				pointer.pop().foreach { case (start, lastMove) =>
					lastMove.foreach { move =>
						val dragEvent = MouseDragEvent2(start.position, move, event.button, start.keyState,
							pressed = false)
						listener.onMouseDrag(dragEvent)
					}
				}
		}
	}
	
	// Updates mouse positions and generates drag events
	override def onMouseMove(event: MouseMoveEvent2): Unit = {
		val events = dragPointers.flatMap { case (button, pointer) =>
			pointer.updateAndGet { _.map { case (start, _) => start -> Some(event) } }
				.map { case (start, _) => MouseDragEvent2(start.position, event, button, start.keyState) }
		}
		events.foreach(listener.onMouseDrag)
	}
	
	
	// NESTED   ------------------------
	
	private case class DragStart(position: RelativePoint, keyState: KeyboardState = KeyboardEvents.state)
}
