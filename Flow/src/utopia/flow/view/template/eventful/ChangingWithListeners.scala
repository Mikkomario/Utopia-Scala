package utopia.flow.view.template.eventful

import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.operator.End
import utopia.flow.view.immutable.View

/**
  * Common trait for changing items that can specify the listeners which are attached to them
  * @author Mikko Hilpinen
  * @since 25.7.2023, v2.2
  */
trait ChangingWithListeners[A] extends Changing[A]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Listeners assigned to this pointer,
	  *         grouped by priority (first the high-priority listeners, then standard priority listeners)
	  */
	protected def listenersByPriority: Pair[Iterable[ChangeListener[A]]]
	
	/**
	  * @param priority Targeted priority group
	  * @param listenersToRemove Listeners to remove from that priority group
	  */
	protected def removeListeners(priority: End, listenersToRemove: Vector[ChangeListener[A]]): Unit
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return Listeners of this changing item that are considered high priority
	  */
	protected def highPriorityListeners = listenersByPriority.first
	/**
	  * @return Listeners of this changing item that are considered standard priority
	  */
	protected def standardListeners = listenersByPriority.second
	/**
	  * @return All listeners of this changing item
	  */
	protected def allListeners = listenersByPriority.flatten
	
	
	// IMPLEMENTED  ----------------------
	
	override def hasListeners: Boolean = listenersByPriority.exists { _.nonEmpty }
	override def numberOfListeners: Int = listenersByPriority.map { _.size }.sum
	
	
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
	protected def fireEventIfNecessary(oldValue: => A, currentValue: => A = value): IndexedSeq[() => Unit] =
		super.fireEventIfNecessary(oldValue, currentValue)(listenersByPriority.apply)(removeListeners)
	
	/**
	  * Informs all listeners about a possible change event.
	  * Updates the list of active listeners accordingly, and performs the applicable after-effects, also
	  * @param lazyEvent A (lazily initialized) pointer/view to the change event that occurred.
	  *                  Contains None in case there was no change after all.
	  *                  Won't be viewed in cases where there are no listeners assigned to this item.
	  * @return After-effects that should be triggered now or later
	  */
	protected def fireEvent(lazyEvent: View[Option[ChangeEvent[A]]]): IndexedSeq[() => Unit] =
		super.fireEvent[A](lazyEvent)(listenersByPriority.apply)(removeListeners)
	
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param event A change event to fire
	  * @return After-effects that should be triggered now or later
	  */
	protected def fireEvent(event: ChangeEvent[A]): IndexedSeq[() => Unit] = fireEvent(View.fixed(Some(event)))
}
