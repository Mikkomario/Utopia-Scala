package utopia.flow.view.template.eventful

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.event.model.ChangeResponse.{Continue, ContinueAnd}
import utopia.flow.operator.End
import utopia.flow.operator.End.{First, Last}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

/**
  * Changing instances generate change events
  * @author Mikko Hilpinen
  * @since 26.5.2019, v1.4.1
  */
abstract class AbstractChanging[A] extends Changing[A]
{
	// ATTRIBUTES   -----------------
	
	// First value contains high priority listeners, second contains standard priority listeners
	private var _listeners = Pair.twice(Vector[ChangeListener[A]]())
	
	
	// COMPUTED --------------------
	
	// TODO: Consider hiding the setters, or at least making them protected
	/**
	  * @return Listeners of this changing item that are considered high priority
	  */
	def highPriorityListeners = _listeners.first
	/**
	  * Replaces the high-priority listeners assigned to this changing item
	  * @param newListeners New set of high-priority listeners
	  */
	def highPriorityListeners_=(newListeners: Vector[ChangeListener[A]]) =
		_listeners = _listeners.withFirst(newListeners)
	/**
	  * @return Listeners of this changing item that are considered standard priority
	  */
	def standardListeners = _listeners.second
	/**
	  * Replaces the standard-priority listeners assigned to this changing item
	  * @param newListeners New set of standard-priority listeners
	  */
	def standardListeners_=(newListeners: Vector[ChangeListener[A]]) =
		_listeners = _listeners.withSecond(newListeners)
	/**
	  * @return All listeners of this changing item
	  */
	def allListeners = _listeners.flatten
	
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
	def listeners_=(newListeners: Vector[ChangeListener[A]]) = standardListeners = newListeners
	
	@deprecated("Replaced with .highPriorityListeners", "v2.2")
	def dependencies = highPriorityListeners
	@deprecated("Replaced with .highPriorityListeners = ...", "v2.2")
	def dependencies_=(newDependencies: Vector[ChangeListener[A]]) = highPriorityListeners = newDependencies
	
	
	// IMPLEMENTED	----------------
	
	override def addListenerOfPriority(priority: End)(listener: => ChangeListener[A]): Unit = {
		// Only adds more listeners if changes are to be anticipated
		if (isChanging)
			_listeners = _listeners.mapSide(priority) { _ :+ listener }
	}
	
	override def removeListener(listener: Any) = _listeners = _listeners.map { _.filterNot { _ == listener } }
	
	
	// OTHER	--------------------
	
	/**
	  * Removes all change listeners from this item
	  */
	protected def clearListeners() = _listeners = Pair.twice(Vector.empty)
	
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param oldValue The old value of this changing element (call-by-name)
	  */
	protected def fireChangeEvent(oldValue: => A) = fireEvent(Lazy { ChangeEvent(oldValue, value) })
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param event A change event to fire
	  */
	protected def fireEvent(event: ChangeEvent[A]): Unit = fireEvent(View.fixed(event))
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param event A change event to fire (possibly lazy)
	  */
	protected def fireEvent(event: View[ChangeEvent[A]]) = {
		// First informs the high priority listeners, then standard priority, and finally performs the after-effects
		End.values.flatMap { _fireEvent(event, _) }.foreach { _() }
	}
	private def _fireEvent(event: View[ChangeEvent[A]], targetedPriority: End) = {
		// Informs the listeners and collects the after effects to trigger later
		// (may remove some listeners in the process)
		val (listenersToRemove, afterEffects) = _listeners(targetedPriority).splitFlatMap { listener =>
			val response = listener.onChangeEvent(event.value)
			(if (response.shouldDetach) Some(listener) else None) -> response.afterEffects
		}
		if (listenersToRemove.nonEmpty)
			_listeners = _listeners.mapSide(targetedPriority) { _.filterNot(listenersToRemove.contains) }
		// Returns the scheduled after-effects
		afterEffects
	}
	
	/**
	  * Starts mirroring another pointer
	  * @param origin Origin value pointer
	  * @param map A value transformation function that accepts two parameters:
	  *            1) Current pointer value,
	  *            2) Source change event,
	  *            and produces a new value to store in this pointer
	  * @param set A function for updating this pointer's value
	  * @tparam O Type of origin pointer's value
	  */
	protected def startMirroring[O](origin: Changing[O])(map: (A, ChangeEvent[O]) => A)(set: A => Unit) = {
		// Registers as a dependency for the origin pointer
		origin.addHighPriorityListener { e1: ChangeEvent[O] =>
			// Whenever the origin's value changes, calculates a new value
			val oldValue = value
			val newValue = map(oldValue, e1)
			// If the new value is different from the previous state, updates the value and generates a new change event
			if (newValue != oldValue) {
				set(newValue)
				// The dependencies are informed immediately, other listeners and after-effects only afterwards
				// TODO: Consider informing these listeners only after the event
				val event2 = Lazy { ChangeEvent(oldValue, newValue) }
				val firstEffects = _fireEvent(event2, First)
				ContinueAnd {
					val moreEffects = _fireEvent(event2, Last)
					(firstEffects.iterator ++ moreEffects).foreach { _() }
				}
			}
			// Case: Projected value didn't change => No change event takes place
			else
				Continue
		}
	}
}