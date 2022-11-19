package utopia.flow.view.template.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.event.model.ChangeEvent
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
	
	/**
	  * Listeners listening this changing instance
	  */
	var listeners = Vector[ChangeListener[A]]()
	/**
	  * Dependencies of this changing instance
	  */
	var dependencies = Vector[ChangeDependency[A]]()
	
	
	// IMPLEMENTED	----------------
	
	override def addListener(changeListener: => ChangeListener[A]) = listeners :+= changeListener
	
	override def addListenerAndSimulateEvent[B >: A](simulatedOldValue: B)(changeListener: => ChangeListener[B]) =
	{
		val newListener = changeListener
		if (simulateChangeEventFor[B](newListener, simulatedOldValue).shouldContinue)
			listeners :+= newListener
	}
	
	override def removeListener(listener: Any) = listeners = listeners.filterNot { _ == listener }
	
	override def addDependency(dependency: => ChangeDependency[A]) = dependencies :+= dependency
	override def removeDependency(dependency: Any) = dependencies = dependencies.filterNot { _ == dependency }
	
	
	// OTHER	--------------------
	
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param oldValue The old value of this changing element (call-by-name)
	  */
	protected def fireChangeEvent(oldValue: => A) = _fireEvent(Lazy { ChangeEvent(oldValue, value) })
	/**
	  * Fires a change event for all the listeners. Informs possible dependencies before informing any listeners.
	  * @param event A change event to fire (should be lazily initialized)
	  */
	protected def fireEvent(event: ChangeEvent[A]) = _fireEvent(View(event))
	private def _fireEvent(event: View[ChangeEvent[A]]) = {
		// Informs the dependencies first
		val afterEffects = dependencies.flatMap { _.beforeChangeEvent(event.value) }
		// Then the listeners (may remove some listeners in the process)
		listeners = listeners.filter { _.onChangeEvent(event.value).shouldContinue }
		// Finally performs the after-effects defined by the dependencies
		afterEffects.foreach { _() }
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
	protected def startMirroring[O](origin: Changing[O])(map: (A, ChangeEvent[O]) => A)(set: A => Unit) =
	{
		// Registers as a dependency for the origin pointer
		origin.addDependency(ChangeDependency { e1: ChangeEvent[O] =>
			// Whenever the origin's value changes, calculates a new value
			val oldValue = value
			val newValue = map(oldValue, e1)
			// If the new value is different from the previous state, updates the value and generates a new change event
			if (newValue != oldValue) {
				set(newValue)
				val event2 = ChangeEvent(oldValue, newValue)
				// The dependencies are informed immediately, other listeners only afterwards
				val afterEffects = dependencies.flatMap { _.beforeChangeEvent(event2) }
				if (afterEffects.nonEmpty || listeners.nonEmpty)
					Some(event2 -> afterEffects)
				else
					None
			}
			else
				None
		} { case (event, actions) =>
			// After the origin has finished its update, informs the listeners and triggers the dependency after-effects
			listeners.foreach { _.onChangeEvent(event) }
			actions.foreach { _() }
		})
	}
}