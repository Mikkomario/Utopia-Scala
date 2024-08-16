package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.Filter
import utopia.flow.operator.sign.Sign
import utopia.genesis.handling.event.consume.{Consumable, ConsumeEvent}
import utopia.genesis.handling.event.mouse.MouseEvent.MouseFilteringFactory
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.VerticalDirection
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

object MouseWheelEvent
{
	// TYPES    ----------------------
	
	/**
	  * Event filter for mouse wheel events
	  */
	type MouseWheelEventFilter = Filter[MouseWheelEvent]
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return Access point to filters targeting mouse wheel events
	  */
	def filter = MouseWheelEventFilter
	
	
	// NESTED   ---------------------
	
	trait MouseWheelFilteringFactory[+A] extends Any with MouseFilteringFactory[MouseWheelEvent, A]
	{
		// COMPUTED ------------------
		
		/**
		  * @return An item that only accepts events where the wheel rotated up / away from the user
		  */
		def rotatedAway = rotated(Up)
		/**
		  * @return An item that only accepts events where the wheel rotated down / towards the user
		  */
		def rotatedTowards = rotated(Down)
		
		/**
		  * @return An item that only accepts unconsumed events
		  */
		def unconsumed = withFilter { _.unconsumed }
		
		
		// OTHER    ------------------
		
		/**
		  * @param rotationDirection Accepted direction of rotation
		  * @return An item that only accepts mouse wheel events towards the specified direction
		  */
		def rotated(rotationDirection: VerticalDirection) =
			withFilter { e => Sign.of(e.wheelTurn) == rotationDirection.sign }
	}
	
	object MouseWheelEventFilter extends MouseWheelFilteringFactory[MouseWheelEventFilter]
	{
		// IMPLEMENTED  --------------
		
		override protected def withFilter(filter: Filter[MouseWheelEvent]): MouseWheelEventFilter = filter
		
		
		// OTHER    ------------------
		
		/**
		  * @param f A filter function
		  * @return A filter that uses the specified function
		  */
		def other(f: MouseWheelEvent => Boolean): MouseWheelEventFilter = Filter(f)
	}
	
	
	// EXTENSIONS   -------------------
	
	implicit class RichMouseWheelEventFilter(val f: MouseWheelEventFilter)
		extends AnyVal with MouseWheelFilteringFactory[MouseWheelEventFilter]
	{
		override protected def withFilter(filter: Filter[MouseWheelEvent]): MouseWheelEventFilter = f && filter
	}
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
case class MouseWheelEvent(wheelTurn: Double, override val position: RelativePoint,
                           override val buttonStates: MouseButtonStates,
                           override val consumeEvent: Option[ConsumeEvent] = None)
	extends MouseEvent[MouseWheelEvent] with Consumable[MouseWheelEvent]
{
	// COMPUTED -----------------------------
	
	/**
	  * @return The amount of "notches" turned "upwards" (i.e. away from the user)
	  */
	def up = -wheelTurn
	/**
	  * @return The amount of "notches" turned "downwards" (i.e. towards the user)
	  */
	def down = wheelTurn
	
	
	// IMPLEMENTED  -------------------------
	
	override def self = this
	
	override def withPosition(position: RelativePoint): MouseWheelEvent = copy(position = position)
	
	override def consumed(event: ConsumeEvent) =
		if (isConsumed) this else copy(consumeEvent = Some(event))
		
	
	// OTHER    -----------------------------
	
	/**
	  * @param direction Targeted direction
	  * @return The amount of wheel turn (in "notches") towards the specified direction
	  */
	def towards(direction: VerticalDirection) = direction.sign * wheelTurn
}