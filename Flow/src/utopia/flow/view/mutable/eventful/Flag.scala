package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.template.eventful.{AbstractChanging, FlagLike}

object Flag
{
	// OTHER    ------------------------
	
	/**
	  * @return A new flag
	  */
	def apply(): Flag = new _Flag()
	
	
	// NESTED   ------------------------
	
	/**
	  * An item that may be set (from false to true) exactly once.
	  * Generates change events when set.
	  * @author Mikko Hilpinen
	  * @since 18.9.2022, v1.17
	  */
	private class _Flag extends AbstractChanging[Boolean] with Flag
	{
		// ATTRIBUTES   ---------------------
		
		private var _value = false
		
		override lazy val view = new FlagView(this)
		override lazy val future = super.future
		
		
		// IMPLEMENTED  ---------------------
		
		override def value = _value
		override def isChanging = isNotSet
		
		// Listeners and dependencies are not accepted after this flag has been set,
		// because they would never be triggered
		override def addListener(changeListener: => ChangeListener[Boolean]) = {
			if (isNotSet)
				super.addListener(changeListener)
		}
		override def addListenerAndSimulateEvent[B >: Boolean](simulatedOldValue: B)(changeListener: => ChangeListener[B]) = {
			if (isSet)
				simulateChangeEventFor(changeListener, simulatedOldValue)
			else
				super.addListenerAndSimulateEvent(simulatedOldValue)(changeListener)
		}
		override def addDependency(dependency: => ChangeDependency[Boolean]) = {
			if (isNotSet)
				super.addDependency(dependency)
		}
		
		
		override def set() = {
			if (isNotSet) {
				_value = true
				fireChangeEvent(false)
				// Forgets all the listeners at this point, because no more change events will be fired
				listeners = Vector()
				dependencies = Vector()
				true
			}
			else
				false
		}
	}
}

/**
  * A common trait for flags.
  * I.e. for boolean containers that may be set from false to true.
  */
trait Flag extends FlagLike
{
	/**
	  * @return An immutable view into this flag
	  */
	def view: FlagLike
	
	/**
	  * Sets this flag (from false to true), unless set already
	  * @return Whether the state of this flag was altered, i.e. whether this flag was not set previously.
	  */
	def set(): Boolean
}
