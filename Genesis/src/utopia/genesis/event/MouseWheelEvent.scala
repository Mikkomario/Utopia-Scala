package utopia.genesis.event

import utopia.genesis.shape.shape2D.Point

/**
 * These mouse events are generated whenever the mouse wheel turns
 * @author Mikko Hilpinen
 * @since 19.2.2017
 * @param wheelTurn The amount of mouse wheel 'notches' since the last event. A positive number
 * indicates a wheel turn towards the user. A negative number indicates a roll away from the user.
  * @param mousePosition Current mouse position.
  * @param absoluteMousePosition Current mouse position in the screen coordinate system (in pixels)
  * @param buttonStatus Current mouse button status
  * @param consumeEvent An event concerning this event's consuming. None if not consumed yet (default)
 */
case class MouseWheelEvent(wheelTurn: Double, override val mousePosition: Point,
						   override val absoluteMousePosition: Point,
						   override val buttonStatus: MouseButtonStatus,
						   override val consumeEvent: Option[ConsumeEvent] = None) extends MouseEvent[MouseWheelEvent]
	with Consumable[MouseWheelEvent]
{
	override def consumed(event: ConsumeEvent) = if (isConsumed) this else copy(consumeEvent = Some(event))
	
	override def me = this
	
	override def mapPosition(f: Point => Point) = copy(mousePosition = f(mousePosition))
}