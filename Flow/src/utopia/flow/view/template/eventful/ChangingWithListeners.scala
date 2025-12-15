package utopia.flow.view.template.eventful

import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponsePriority.{High, Normal}
import utopia.flow.event.model.{AfterEffect, ChangeEvent, ChangeResponsePriority}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

/**
  * Common trait for changing items that can specify the listeners which are attached to them
  * @author Mikko Hilpinen
  * @since 25.7.2023, v2.2
  */
trait ChangingWithListeners[A] extends Changing[A]
{
	// ABSTRACT --------------------------
	
	/**
	 * @param priority Targeted change response -priority level
	 * @return All listeners attached to this pointer, that belong to that priority group
	 */
	protected def listenersOfPriority(priority: ChangeResponsePriority): IterableOnce[ChangeListener[A]]
	/**
	  * @param priority Targeted priority group
	  * @param listenerToRemove Listener to remove from that priority group
	  */
	protected def removeListener(priority: ChangeResponsePriority, listenerToRemove: ChangeListener[A]): Unit
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Listeners of this changing item that are considered high priority
	  */
	@deprecated("Please use listenersOfPriority(High) instead", "v2.8")
	protected def highPriorityListeners = listenersOfPriority(High)
	/**
	  * @return Listeners of this changing item that are considered standard priority
	  */
	@deprecated("Please use listenersOfPriority(Normal) instead", "v2.8")
	protected def standardListeners = listenersOfPriority(Normal)
	/**
	  * @return All listeners of this changing item
	  */
	@deprecated("Deprecated for removal", "v2.8")
	protected def allListeners =
		ChangeResponsePriority.descending.flatMap(listenersOfPriority)
	
	
	// IMPLEMENTED  ----------------------
	
	override def hasListeners: Boolean =
		ChangeResponsePriority.descending.exists { listenersOfPriority(_).iterator.hasNext }
	override def numberOfListeners: Int = ChangeResponsePriority.descending.iterator
		.map { prio =>
			listenersOfPriority(prio) match {
				case i: Iterable[_] => i.size
				case i => i.iterator.size
			}
		}
		.sum
	
	
	// OTHER    --------------------------
	
	/**
	  * Informs all listeners about a possible change event.
	  * Updates the list of active listeners accordingly, and performs the applicable after-effects, also.
	  *
	  * Won't generate any event or action in cases where the old and the new value would resolve in the same value,
	  * nor in cases where no listeners have been assigned.
	  *
	  * @param oldValue     The previous value of this changing item
	  *                     (call-by-name, called only if there are listeners assigned to this item)
	  * @param currentValue Current value of this changing item
	  *                     (call-by-name, called only if there are listeners assigned to this item)
	  *                     (default = current value of this pointer)
	  * @return After-effects that should be triggered now or later
	  */
	@deprecated("Deprecated for removal. Please use .fireEvent(View[ChangeEvent]) instead", "v2.8")
	protected def fireEventIfNecessary(oldValue: A, currentValue: A = value): Unit = {
		if (oldValue != currentValue)
			fireEvent(Lazy { ChangeEvent(oldValue, currentValue) })
	}
	
	/**
	 * Fires a (root level) change event from this pointer,
	 * informing all listeners and triggering all the generated after effects.
	 *
	 * Note: If this event is generated as a response to another change event, please use [[fireEventEffects]] instead.
	 *
	 * @param event A change event to fire
	 */
	protected def fireEvent(event: ChangeEvent[A]): Unit = fireEvent(View.fixed(event))
	/**
	  * Fires a (root level) change event from this pointer,
	 * informing all listeners and triggering all the generated after effects.
	 *
	 * Note: If this event is generated as a response to another change event, please use [[fireEventEffects]] instead.
	 *
	  * @param lazyEvent A change event to fire, wrapped in a lazily initialized container
	  */
	protected def fireEvent(lazyEvent: View[ChangeEvent[A]]): Unit =
		Changing.fireRootEvent(lazyEvent)(listenersOfPriority)(removeListener)
	
	/**
	 * Prepares to fire change events as after-effects in response to another change event.
	 * @param event Event that's to be fired
	 * @return An iterator that yields the after-effects to trigger when handling the original change event.
	 */
	protected def fireEventEffects(event: ChangeEvent[A]): Iterator[AfterEffect] = fireEventEffects(View.fixed(event))
	/**
	 * Prepares to fire change events as after-effects in response to another change event.
	 * @param lazyEvent Event that's fired, wrapped in a lazy container.
	 *                  Called if/when some listener needs to be informed.
	 * @return An iterator that yields the after-effects to trigger when handling the original change event.
	 */
	protected def fireEventEffects(lazyEvent: View[ChangeEvent[A]]) =
		Changing.prepareFireEventEffects(lazyEvent)(listenersOfPriority)(removeListener)
}
