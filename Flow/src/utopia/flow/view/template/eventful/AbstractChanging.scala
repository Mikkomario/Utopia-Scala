package utopia.flow.view.template.eventful

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.event.model.ChangeResponsePriority.{High, Normal}
import utopia.flow.event.model.{ChangeEvent, ChangeResponsePriority}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.async.Volatile

/**
  * An abstract implementation of the [[Changing]] trait. Handles [[ChangeListener]] handling.
  * Suitable as a parent for all types of Changing implementations, except for those based on wrapping.
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1
  */
abstract class AbstractChanging[A](implicit override val listenerLogger: Logger) extends ChangingWithListeners[A]
{
	// ATTRIBUTES   -----------------
	
	private val listenerPointers = ChangeResponsePriority.descending.iterator
		.map { _ -> Volatile.emptySeq[ChangeListener[A]] }.toMap
	
	
	// COMPUTED --------------------
	
	/**
	  * Replaces the high-priority listeners assigned to this changing item
	  * @param newListeners New set of high-priority listeners
	  */
	@deprecated("Deprecated for removal", "v2.8")
	protected def highPriorityListeners_=(newListeners: Seq[ChangeListener[A]]) =
		listenerPointers(High).value = newListeners
	/**
	  * Replaces the standard-priority listeners assigned to this changing item
	  * @param newListeners New set of standard-priority listeners
	  */
	@deprecated("Deprecated for removal", "v2.8")
	protected def standardListeners_=(newListeners: Seq[ChangeListener[A]]) =
		listenerPointers(Normal).value = newListeners
	
	
	// IMPLEMENTED	----------------
	
	override protected def listenersOfPriority(priority: ChangeResponsePriority): IterableOnce[ChangeListener[A]] =
		listenerPointers(priority).value
	
	override protected def removeListener(priority: ChangeResponsePriority, listenerToRemove: ChangeListener[A]): Unit =
		listenerPointers(priority).update { _.filterNot { _ == listenerToRemove } }
	override def removeListener(changeListener: Any): Unit =
		ChangeResponsePriority.ascendingIterator.find { prio =>
			listenerPointers(prio).mutate { _.findAndPop { _ == changeListener } }.isDefined
		}
	
	override protected def _addListenerOfPriority(priority: ChangeResponsePriority, lazyListener: View[ChangeListener[A]]): Unit =
		listenerPointers(priority).update { listeners =>
			// Only adds more listeners if the listener is unique
			val listener = lazyListener.value
			if (listeners.contains(listener)) listeners else listeners :+ listener
		}
	
	override def lockWhile[B](operation: => B): B = this.synchronized(operation)
	
	
	// OTHER	--------------------
	
	/**
	  * Removes all change listeners from this item
	  */
	// TODO: Should this really be public?
	def clearListeners() = listenerPointers.valuesIterator.foreach { _.clear() }
	
	/**
	  * Starts mirroring another pointer
	  * @param origin Origin value pointer
	  * @param condition A condition that must be met for the mirroring to take place / update (default = always active)
	  * @param map A value transformation function that accepts two parameters:
	  *            1) Current pointer value,
	  *            2) Source change event,
	  *            and produces a new value to store in this pointer
	  * @param set A function for updating this pointer's value
	  * @tparam O Type of origin pointer's value
	  */
	protected def startMirroring[O](origin: Changing[O], condition: Changing[Boolean] = AlwaysTrue)
	                               (map: (A, ChangeEvent[O]) => A)
	                               (set: A => Unit) =
	{
		// Registers as a dependency for the origin pointer
		origin.addListenerWhile(condition, priority = High) { e1: ChangeEvent[O] =>
			// Whenever the origin's value changes, calculates a new value
			val oldValue = value
			val newValue = map(oldValue, e1)
			
			// If the new value is different from the previous state, updates the value and generates a new change event
			if (newValue != oldValue) {
				set(newValue)
				Continue ++ fireEventEffects(ChangeEvent(oldValue, newValue))
			}
			// Case: Projected value didn't change => No change event takes place
			else
				Continue
		}
	}
}