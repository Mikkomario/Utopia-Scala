package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter, RejectAll}
import utopia.flow.time.TimeExtensions._
import utopia.genesis.handling.event.consume.{Consumable, ConsumeEvent}
import utopia.genesis.handling.event.mouse.MouseEvent.MouseFilteringFactory
import utopia.paradigm.shape.shape2d.vector.point.RelativePoint

import scala.concurrent.duration.{Duration, FiniteDuration}

object MouseOverEvent
{
	// TYPES    ------------------------
	
	/**
	  * Type of filters applied to mouse over events
	  */
	type MouseOverEventFilter = Filter[MouseOverEvent]
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return An access point to mouse over event filters
	  */
	def filter = MouseOverEventFilter
	
	
	// NESTED   ------------------------
	
	trait MouseOverFilteringFactory[+A] extends Any with MouseFilteringFactory[MouseOverEvent, A]
	{
		/**
		  * @return An item that only accepts unconsumed events
		  */
		def unconsumed = withFilter { _.unconsumed }
		
		/**
		  * @param minimumDuration Minimum hover duration
		  * @return An item that only accepts events once the hover extends over the specified duration
		  */
		def longerThan(minimumDuration: Duration) = {
			if (minimumDuration <= Duration.Zero)
				withFilter(AcceptAll)
			else
				minimumDuration.finite match {
					case Some(duration) => withFilter { _.totalDuration >= duration }
					case None => withFilter(RejectAll)
				}
		}
	}
	
	object MouseOverEventFilter extends MouseOverFilteringFactory[MouseOverEventFilter]
	{
		// IMPLEMENTED  -----------------------
		
		override protected def withFilter(filter: Filter[MouseOverEvent]): MouseOverEventFilter = filter
		
		
		// OTHER    ---------------------------
		
		/**
		  * @param f A filtering function
		  * @return A filter that uses the specified function
		  */
		def apply(f: MouseOverEvent => Boolean) = Filter(f)
	}
	
	
	// EXTENSIONS   ---------------------------
	
	implicit class RichMouseOverEventFilter(val f: MouseOverEventFilter)
		extends AnyVal with MouseOverFilteringFactory[MouseOverEventFilter]
	{
		override protected def withFilter(filter: Filter[MouseOverEvent]): MouseOverEventFilter = f && filter
	}
}

/**
  * These events are fired when the mouse hovers over an object
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
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
	extends MouseEvent[MouseOverEvent] with Consumable[MouseOverEvent]
{
	override def self: MouseOverEvent = this
	
	override def withPosition(position: RelativePoint): MouseOverEvent = copy(position = position)
	override def consumed(consumeEvent: ConsumeEvent): MouseOverEvent =
		if (isConsumed) this else copy(consumeEvent = Some(consumeEvent))
}
