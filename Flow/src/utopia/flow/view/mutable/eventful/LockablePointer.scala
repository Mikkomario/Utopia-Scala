package utopia.flow.view.mutable.eventful

import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.{LoggingPointerFactory, MaybeAssignable}
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper}

import scala.util.Try

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
		
		override def lock() = {
			_locked = true
			declareChangingStopped()
		}
		
		override protected def assignToUnlocked(newValue: A): Unit = {
			val oldValue = _value
			_value = newValue
			fireEventIfNecessary(oldValue, newValue).foreach { effect => Try { effect() }.log }
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
	
	
	// IMPLEMENTED  -------------------------
	
	@throws[IllegalStateException]("If this pointer has been locked")
	override def value_=(newValue: A): Unit = {
		if (locked)
			throw new IllegalStateException("This pointer has been locked")
		else
			assignToUnlocked(newValue)
	}
	override def set(value: A): Unit = this.value = value
	
	/**
	  * Attempts to modify the value of this pointer.
	  * Won't modify the value if this pointer has been locked.
	  * @param value A value that should be set to this pointer, if possible
	  * @return Whether the value of this pointer was modified
	  */
	override def trySet(value: => A) = {
		if (locked)
			false
		else {
			assignToUnlocked(value)
			true
		}
	}
}
