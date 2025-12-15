package utopia.flow.view.mutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.model.{AfterEffect, ChangeEvent}
import utopia.flow.util.Mutate
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.{LoggingPointerFactory, MaybeAssignable}
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper}

object LockablePointer extends LoggingPointerFactory[LockablePointer]
{
	// IMPLEMENTED    ----------------------
	
	/**
	  * @param initialValue Initial value to assign to this pointer
	  * @param log Logging implementation for handling failures in change-event -handling
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	override def apply[A](initialValue: A)(implicit log: Logger): LockablePointer[A] =
		new _LockablePointer[A](initialValue)
	
	
	// NESTED   ---------------------------
	
	private class _LockablePointer[A](initialValue: A)(implicit log: Logger)
		extends AbstractMayStopChanging[A] with LockablePointer[A]
	{
		// ATTRIBUTES   -------------------------
		
		private var _value = initialValue
		private var _locked = false
		
		override lazy val readOnly: Changing[A] = ChangingWrapper(this)
		
		
		// IMPLEMENTED  -------------------------
		
		override def value: A = _value
		override def locked = _locked
		
		override def toString = if (_locked) s"Pointer(${ _value }).locked" else s"Pointer(${ _value }).lockable"
		
		override def lock() = {
			_locked = true
			declareChangingStopped()
		}
		
		override protected def assignToUnlocked(newValue: A): Unit = {
			val oldValue = _value
			_value = newValue
			if (oldValue != newValue)
				fireEvent(ChangeEvent(oldValue, newValue))
		}
		override protected def setUnlockedAndQueueEvent(newValue: A): IterableOnce[AfterEffect] = {
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
  * This mutable pointer class allows one to flag it as "locked",
  * after which it can't be modified anymore, and is considered static in terms of change-listener handling.
  * @author Mikko Hilpinen
  * @since 26.7.2023, v2.2
  */
trait LockablePointer[A] extends Lockable[A] with EventfulPointer[A] with MaybeAssignable[A]
{
	// ABSTRACT -----------------------------
	
	/**
	 * Assigns a new value to this pointer, also firing change events.
	 * This method is only called for non-locked pointers.
	 * @param newValue New value to assign to this pointer
	 */
	protected def assignToUnlocked(newValue: A): Unit
	/**
	 * Assigns a new value to this pointer. Prepares, but doesn't fire the change events.
	 * This method is only called for non-locked pointers.
	 * @param newValue New value to assign to this pointer
	 * @return The firing of the change-event, if appropriate, as after-effects
	 */
	protected def setUnlockedAndQueueEvent(newValue: A): IterableOnce[AfterEffect]
	
	
	// IMPLEMENTED  -------------------------
	
	@throws[IllegalStateException]("If this pointer has been locked")
	override def value_=(newValue: A): Unit = failIfLocked { assignToUnlocked(newValue) }
	override def set(value: A): Unit = this.value = value
	override def setAndQueueEvent(newValue: A): IterableOnce[AfterEffect] =
		failIfLocked { setUnlockedAndQueueEvent(newValue) }
	
	/**
	  * Attempts to modify the value of this pointer.
	  * Won't modify the value if this pointer has been locked.
	  * @param value A value that should be set to this pointer, if possible
	  * @return Whether the value of this pointer was modified
	  */
	override def trySet(value: => A) = ifUnlocked { assignToUnlocked(value) }
	
	
	// OTHER    ----------------------------
	
	/**
	  * Attempts to update/mutate the value of this pointer.
	  * Won't modify this pointer if already locked.
	  * @param f A function that modifies the held value of this pointer
	  * @return Whether this pointer was still unlocked and 'f' was called.
	  */
	def tryUpdate(f: Mutate[A]) = ifUnlocked { update(f) }
}
