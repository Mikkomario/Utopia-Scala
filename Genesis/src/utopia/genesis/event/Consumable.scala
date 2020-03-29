package utopia.genesis.event

import utopia.inception.util.Filter

object Consumable
{
	val notConsumedFilter: Filter[Consumable[_]] = !_.isConsumed
}

/**
  * These items may be consumed. Usually used with events when one wants to inform other listeners that the event
  * has already been 'used'
  * @author Mikko Hilpinen
  * @since 10.5.2019, v2.1+
  */
trait Consumable[+Repr]
{
	// ABSTRACT	---------------------
	
	/**
	  * @return An consume event if this item has already been consumed
	  */
	def consumeEvent: Option[ConsumeEvent]
	/**
	  * @return This item as 'Repr'
	  */
	def me: Repr
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
	
	
	// OTHER	--------------------
	
	/**
	  * @param by Description of entity that consumed this event
	  * @return A consumed copy of this consumable (call by name)
	  */
	def consumed(by: => String): Repr = consumed(new ConsumeEvent(() => by))
	
	/**
	  * Handles this consumable item using a number of possibly consuming operations
	  * @param hasNext A function that returns whether there are still operations left
	  * @param take Performs a single operation on this consumable item. Returns a consume event if this event is / was consumed.
	  * @return Whether this event should now be considered consumed
	  */
	def handleWith(hasNext: => Boolean)(take: Repr => Option[ConsumeEvent]): Option[ConsumeEvent] =
	{
		if (isConsumed)
		{
			while (hasNext) { take(me) }
			consumeEvent
		}
		else
		{
			var con: Option[ConsumeEvent] = None
			var event = me
			while (hasNext)
			{
				if (con.isDefined)
					take(event)
				else
				{
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
	def distributeAmong[L](listeners: Seq[L])(call: (L, Repr) => Option[ConsumeEvent]) =
	{
		var nextIndex = 0
		def hasNext = nextIndex < listeners.size
		def take(c: Repr) =
		{
			nextIndex += 1
			call(listeners(nextIndex - 1), c)
		}
		
		handleWith(hasNext)(take)
	}
}
