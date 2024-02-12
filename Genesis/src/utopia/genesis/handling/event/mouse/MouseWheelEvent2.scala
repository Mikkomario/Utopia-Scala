package utopia.genesis.handling.event.mouse

import utopia.genesis.handling.event.consume.{Consumable, ConsumeEvent}
import utopia.genesis.handling.event.mouse.MouseWheelListener2.MouseWheelEventFilter
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

object MouseWheelEvent2
{
	/**
	  * @return Access point to filters targeting mouse wheel events
	  */
	def filter = MouseWheelEventFilter
}

/**
 * These mouse events are generated whenever the mouse wheel turns
 * @author Mikko Hilpinen
 * @since 19.2.2017
 * @param wheelTurn The amount of mouse wheel 'notches' since the last event. A positive number
 * indicates a wheel turn towards the user. A negative number indicates a roll away from the user.
  * @param position Mouse coordinates during this event
  * @param buttonStates Mouse button states during this event
  * @param consumeEvent An event concerning this event's consuming. None if not consumed yet (default)
 */
case class MouseWheelEvent2(wheelTurn: Double, override val position: RelativePoint,
                            override val buttonStates: MouseButtonStates,
                            override val consumeEvent: Option[ConsumeEvent] = None)
	extends MouseEvent2[MouseWheelEvent2] with Consumable[MouseWheelEvent2]
{
	override def self = this
	
	override def withPosition(position: RelativePoint): MouseWheelEvent2 = copy(position = position)
	
	override def consumed(event: ConsumeEvent) =
		if (isConsumed) this else copy(consumeEvent = Some(event))
}