package utopia.genesis.handling.event.consume

import utopia.flow.operator.Identity
import utopia.flow.operator.filter.Filter
import utopia.flow.util.Mutate
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.eventful.SettableOnce
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}

object Consumable
{
	// ATTRIBUTES   -----------------
	
	/**
	  * A filter that only accepts unconsumed items
	  */
	val unconsumedFilter: Filter[Consumable[_]] = _.unconsumed
	
	
	// COMPUTED ---------------------
	
	@deprecated("Please use .unconsumedFilter instead", "v4.0")
	def notConsumedFilter = unconsumedFilter
}

/**
  * Common trait for items that may be consumed.
  * Usually used with events when one wants to inform other listeners that the event has already been 'used'
  * @author Mikko Hilpinen
  * @since 10.5.2019, v2.1+
  */
trait Consumable[+Repr]
{
	// ABSTRACT	---------------------
	
	/**
	  * @return This item as 'Repr'
	  */
	def self: Repr
	
	/**
	  * @return An consume event if this item has already been consumed.
	  *         None if this item hasn't been consumed.
	  */
	def consumeEvent: Option[ConsumeEvent]
	
	/**
	  * @param consumeEvent A consume event
	  * @return A consumed version of this item
	  */
	def consumed(consumeEvent: ConsumeEvent): Repr
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this event has already been consumed
	  */
	def isConsumed = consumeEvent.isDefined
	/**
	  * @return Whether this event hasn't been consumed yet
	  */
	def unconsumed = !isConsumed
	
	
	// OTHER	--------------------
	
	/**
	  * @param by Description of entity that consumed this event (call-by-name)
	  * @return A consumed copy of this consumable
	  */
	def consumed(by: => String): Repr = consumed(new ConsumeEvent(by))
	
	/**
	  * @param consumeChoice A choice to either consume or preserve this event as it is
	  * @return Copy of this event that has been consumed, if the choice was to make it so
	  */
	def after(consumeChoice: ConsumeChoice): Repr = {
		if (isConsumed)
			self
		else
			consumeChoice match {
				case Consume(consumeEvent) => consumed(consumeEvent)
				case Preserve => self
			}
	}
	
	/**
	  * Distributes this event among the specified listeners.
	  * If one of the listeners chooses to consume this event, the remaining listeners will receive a consumed copy.
	  * @param listeners Listeners to inform
	  * @param deliver A function which delivers this event to a listener
	  * @tparam L Type of listeners used
	  * @return Copy of this event after the deliveries, plus a consume choice to forward, if necessary.
	  *         If one of the listeners consumed this event, returns the consumed copy. Otherwise returns this event.
	  */
	def distribute[L](listeners: IterableOnce[L])(deliver: (L, Repr) => ConsumeChoice) =
		transformAndDistribute(listeners.iterator.map[(L, Mutate[Repr])] { _ -> Identity })(deliver)
	/**
	  * Distributes this event among the specified listeners.
	  * Applies a custom modify function to each distribution.
	  * If one of the listeners chooses to consume this event, the remaining listeners will receive a consumed copy.
	  * @param listeners Listeners to inform.
	  *                  Each listener is coupled with a transformation function that
	  *                  customizes the delivered event for them.
	  * @param deliver A function which delivers this event to a listener
	  * @tparam L Type of listeners used
	  * @return Copy of this event after the deliveries, plus a consume choice to forward, if necessary.
	  *         If one of the listeners consumed this event, returns the consumed copy. Otherwise returns this event.
	  */
	def transformAndDistribute[L, E >: Repr](listeners: IterableOnce[(L, Repr => E)])(deliver: (L, E) => ConsumeChoice) = {
		val listenerIter = listeners.iterator
		// Case: No listeners to inform => No-op
		if (!listenerIter.hasNext)
			self -> Preserve
		// Case: Already consumed => Won't bother tracking further consume events
		else if (isConsumed) {
			listenerIter.foreach { case (l, t) => deliver(l, t(self)) }
			self -> Preserve
		}
		// Case: Not yet consumed => Prepares for a possible consume event
		else {
			implicit val log: Logger = SysErrLogger
			
			// Swaps this event to a consumed copy once a consume event has been received
			val consumeEventPointer = SettableOnce[ConsumeEvent]()
			val eventPointer = consumeEventPointer.strongMap {
				case Some(consumeEvent) => consumed(consumeEvent)
				case None => self
			}
			// Informs the listeners in order
			listenerIter.foreach { case (l, t) =>
				val response = deliver(l, t(eventPointer.value))
				if (consumeEventPointer.value.isEmpty)
					consumeEventPointer.value = response.eventIfConsumed
			}
			eventPointer.value -> ConsumeChoice(consumeEventPointer.value)
		}
	}
	
	/**
	  * Handles this consumable item using a number of possibly consuming operations
	  * @param hasNext A function that returns whether there are still operations left
	  * @param take Performs a single operation on this consumable item. Returns a consume event if this event is / was consumed.
	  * @return Whether this event should now be considered consumed
	  */
	@deprecated("Please convert to using distributeAmong(...) instead", "v4.0")
	def handleWith(hasNext: => Boolean)(take: Repr => Option[ConsumeEvent]): Option[ConsumeEvent] = {
		if (isConsumed) {
			while (hasNext) { take(self) }
			consumeEvent
		}
		else {
			var con: Option[ConsumeEvent] = None
			var event = self
			while (hasNext) {
				if (con.isDefined)
					take(event)
				else {
					con = take(event)
					if (con.isDefined)
						event = consumed(con.get)
				}
			}
			con
		}
	}
	/**
	  * Distributes this event among multiple listeners that may consume this event
	  * @param listeners The listeners
	  * @param call A function that informs a single listener about this event
	  * @tparam L The type of the listeners
	  * @return Whether this event should now be considered consumed
	  */
	@deprecated("Please convert to using distribute(...) instead", "v4.0")
	def distributeAmong[L](listeners: Seq[L])(call: (L, Repr) => Option[ConsumeEvent]) = {
		var nextIndex = 0
		def hasNext = nextIndex < listeners.size
		def take(c: Repr) = {
			nextIndex += 1
			call(listeners(nextIndex - 1), c)
		}
		
		handleWith(hasNext)(take)
	}
}
