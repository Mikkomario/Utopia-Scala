package utopia.flow.view.template.eventful

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.logging.Logger
import utopia.flow.util.TryExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue

import scala.util.Try

/**
  * An abstract implementation of the [[Changing]] trait. Handles [[ChangeListener]] handling.
  * Suitable as a parent for all types of Changing implementations, except for those based on wrapping.
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1
  */
abstract class AbstractChanging[A](implicit override val listenerLogger: Logger) extends ChangingWithListeners[A]
{
	// ATTRIBUTES   -----------------
	
	// First value contains high priority listeners, second contains standard priority listeners
	private var _listeners = Pair.twice[Seq[ChangeListener[A]]](Empty)
	
	
	// COMPUTED --------------------
	
	/**
	  * Replaces the high-priority listeners assigned to this changing item
	  * @param newListeners New set of high-priority listeners
	  */
	protected def highPriorityListeners_=(newListeners: Seq[ChangeListener[A]]) =
		_listeners = _listeners.withFirst(newListeners)
	/**
	  * Replaces the standard-priority listeners assigned to this changing item
	  * @param newListeners New set of standard-priority listeners
	  */
	protected def standardListeners_=(newListeners: Seq[ChangeListener[A]]) =
		_listeners = _listeners.withSecond(newListeners)
	
	
	// IMPLEMENTED	----------------
	
	override protected def listenersByPriority: Pair[Iterable[ChangeListener[A]]] = _listeners
	
	override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit = {
		// Only adds more listeners if the listener is unique
		val newListener = lazyListener.value
		_listeners = _listeners.mapSide(priority) { q => if (q.contains(newListener)) q else q :+ newListener }
	}
	
	override def removeListener(listener: Any) = _listeners = _listeners.map { _.filterNot { _ == listener } }
	override protected def removeListener(priority: End, listenerToRemove: ChangeListener[A]): Unit =
		_listeners = _listeners.mapSide(priority) { _.filterNot { _ == listenerToRemove } }
	
	override def lockWhile[B](operation: => B): B = this.synchronized(operation)
	
	
	// OTHER	--------------------
	
	/**
	  * Removes all change listeners from this item
	  */
	def clearListeners() = _listeners = Pair.twice(Empty)
	
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
		origin.addListenerWhile(condition, priority = First) { e1: ChangeEvent[O] =>
			// Whenever the origin's value changes, calculates a new value
			val oldValue = value
			val newValue = map(oldValue, e1)
			
			// If the new value is different from the previous state, updates the value and generates a new change event
			if (newValue != oldValue) {
				set(newValue)
				// The dependencies are informed immediately, other listeners and after-effects only afterwards
				// TODO: Consider informing these listeners only after the event
				val event2 = ChangeEvent(oldValue, newValue)
				val firstEffects = _fireEvent(Some(event2), First)
				Continue.and {
					val moreEffects = _fireEvent(Some(event2), Last)
					(firstEffects.iterator ++ moreEffects)
						.foreach { e => Try { e() }.logWithMessage("Failure during a change effect") }
				}
			}
			// Case: Projected value didn't change => No change event takes place
			else
				Continue
		}
	}
	
	private def _fireEvent(event: => Option[ChangeEvent[A]], targetedPriority: End) =
		fireEventFor(_listeners(targetedPriority), event) { removeListener(targetedPriority, _) }
}