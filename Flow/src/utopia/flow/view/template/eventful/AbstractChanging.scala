package utopia.flow.view.template.eventful

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.End
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue

/**
  * Changing instances generate change events
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1
  */
abstract class AbstractChanging[A] extends ChangingWithListeners[A]
{
	// ATTRIBUTES   -----------------
	
	// First value contains high priority listeners, second contains standard priority listeners
	private var _listeners = Pair.twice[Seq[ChangeListener[A]]](Empty)
	
	// Caches the withState -version
	override lazy val withState = super.withState
	
	
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
	
	/**
	  * @return Listeners that are informed of changes within this item
	  */
	@deprecated("Replaced with .standardListeners and .allListeners", "v2.2")
	def listeners = standardListeners
	/**
	  * Replaces the listeners of this changing item
	  * @param newListeners New listeners to assign
	  */
	@deprecated("Replaced with .standardListeners = ...", "v2.2")
	def listeners_=(newListeners: Seq[ChangeListener[A]]) = standardListeners = newListeners
	
	@deprecated("Replaced with .highPriorityListeners", "v2.2")
	def dependencies = highPriorityListeners
	@deprecated("Replaced with .highPriorityListeners = ...", "v2.2")
	def dependencies_=(newDependencies: Seq[ChangeListener[A]]) = highPriorityListeners = newDependencies
	
	
	// IMPLEMENTED	----------------
	
	override protected def listenersByPriority: Pair[Iterable[ChangeListener[A]]] = _listeners
	
	override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit = {
		// Only adds more listeners if the listener is unique
		val newListener = lazyListener.value
		_listeners = _listeners.mapSide(priority) { q => if (q.contains(newListener)) q else q :+ newListener }
	}
	
	override def removeListener(listener: Any) = _listeners = _listeners.map { _.filterNot { _ == listener } }
	override protected def removeListeners(priority: End, listenersToRemove: Iterable[ChangeListener[A]]): Unit =
		_listeners = _listeners.mapSide(priority) { _.filterNot { l => listenersToRemove.exists { _ == l } } }
	
	
	// OTHER	--------------------
	
	/**
	  * Removes all change listeners from this item
	  */
	def clearListeners() = _listeners = Pair.twice(Empty)
	
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param oldValue The old value of this changing element (call-by-name)
	  */
	@deprecated("Replaced with fireEventIfNecessary(A, A)", "v2.2")
	protected def fireChangeEvent(oldValue: => A) = fireEventIfNecessary(oldValue, value).foreach { _() }
	
	private def _fireEvent(event: => Option[ChangeEvent[A]], targetedPriority: End) = {
		// Informs the listeners and collects the after effects to trigger later
		// (may remove some listeners in the process)
		val responses = fireEventFor(_listeners(targetedPriority), event)
		val listenersToRemove = responses.flatMap { case (l, r) => if (r.shouldDetach) Some(l) else None }
		if (listenersToRemove.nonEmpty)
			removeListeners(targetedPriority, listenersToRemove)
		// Returns the scheduled after-effects
		responses.flatMap { _._2.afterEffects }
	}
	
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
					(firstEffects.iterator ++ moreEffects).foreach { _() }
				}
			}
			// Case: Projected value didn't change => No change event takes place
			else
				Continue
		}
	}
}