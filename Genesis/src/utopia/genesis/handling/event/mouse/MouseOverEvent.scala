package utopia.genesis.handling.event.mouse

import utopia.genesis.event.{Consumable, ConsumeEvent}
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.FiniteDuration

/**
  * These events are fired when the mouse hovers over an object
  * @author Mikko Hilpinen
  * @since 06/02/2024, v3.6
  * @param position Current mouse hover position
  * @param buttonStates Current mouse button states
  * @param duration Duration since the last event / check (based on action events)
  * @param totalDuration How long the mouse has hovered over this object so far
  * @param consumeEvent An event concerning the consumption of this event.
  *                     None if this event hasn't yet been consumed.
  */
case class MouseOverEvent(override val position: RelativePoint, override val buttonStates: MouseButtonStates,
                          duration: FiniteDuration, totalDuration: FiniteDuration,
                          override val consumeEvent: Option[ConsumeEvent] = None)
	extends MouseEvent2[MouseOverEvent] with Consumable[MouseOverEvent]
{
	override def self: MouseOverEvent = this
	
	override def withPosition(position: RelativePoint): MouseOverEvent = copy(position = position)
	override def consumed(consumeEvent: ConsumeEvent): MouseOverEvent =
		if (isConsumed) this else copy(consumeEvent = Some(consumeEvent))
}
