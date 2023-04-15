package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper, FlagLike}

import scala.concurrent.Future

object Flag
{
	// OTHER    ------------------------
	
	/**
	  * @return A new flag
	  */
	def apply(): Flag = new _Flag()
	
	/**
	  * Wraps a flag view, adding a mutable interface to it
	  * @param view A view that provides the state of this flag
	  * @param set A function for setting this flag.
	  *            Returns whether the state was altered.
	  * @return A new flag that wraps the specified view and uses the specified function to alter state.
	  */
	def wrap(view: Changing[Boolean])(set: => Boolean): Flag = new IndirectFlag(view, () => set)
	
	
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
		
		// Can't be set twice, so asking for nextFuture after set is futile
		override def nextFuture = if (isSet) Future.never else future
		
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
	
	private class IndirectFlag(override protected val wrapped: Changing[Boolean], indirectSet: () => Boolean)
		extends Flag with ChangingWrapper[Boolean]
	{
		override lazy val view: FlagLike = wrapped match {
			case f: FlagLike => f
			case o => o
		}
		
		override def set(): Boolean = indirectSet()
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
