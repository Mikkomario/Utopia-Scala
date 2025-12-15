package utopia.flow.view.mutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.model.{AfterEffect, ChangeEvent}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Flag}

object ResettableLockableFlag
{
	// OTHER    -------------------------------
	
	/**
	  * @param initialState Initial state to assign for this flag
	  * @param log Implicit logging implementation used for handling errors during event-handling
	  * @return A new flag that may be reset and/or locked
	  */
	def apply(initialState: Boolean = false)(implicit log: Logger): ResettableLockableFlag =
		new _ResettableLockableFlag(initialState)
	
	
	// NESTED   -------------------------------
	
	private class _ResettableLockableFlag(initialValue: Boolean)(implicit log: Logger)
		extends AbstractMayStopChanging[Boolean] with ResettableLockableFlag
	{
		// ATTRIBUTES   -----------------------
		
		private var _value = initialValue
		private var _locked = false
		
		override lazy val view: Flag = new FlagView(this)
		
		
		// IMPLEMENTED  -----------------------
		
		override def value: Boolean = _value
		override def locked: Boolean = _locked
		
		override def toString = {
			if (_locked) {
				if (_value)
					"Flag.set.locked"
				else
					"Flag.locked"
			}
			else
				s"Flag(${ _value }).resettable.lockable"
		}
		
		override def lock(): Unit = {
			if (!locked) {
				_locked = true
				declareChangingStopped()
			}
		}
		
		override protected def assignToUnlocked(newValue: Boolean): Unit = {
			val oldValue = _value
			_value = newValue
			if (oldValue != newValue)
				fireEvent(ChangeEvent(oldValue, newValue))
		}
		override protected def setUnlockedAndQueueEvent(newValue: Boolean): IterableOnce[AfterEffect] = {
			val oldValue = _value
			_value = newValue
			if (oldValue == newValue)
				Empty
			else
				fireEventEffects(ChangeEvent(oldValue, newValue))
		}
	}
}

/**
  * Common traits that may be reset and locked.
  * @author Mikko Hilpinen
  * @since 30.03.2025, v2.6
  */
trait ResettableLockableFlag extends ResettableFlag with LockableFlag with LockablePointer[Boolean]