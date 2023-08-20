package utopia.flow.view.mutable.eventful

import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing}

object LockablePointer
{
	// OTHER    --------------------------
	
	/**
	  * @param initialValue Initial value to assign to this pointer
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	def apply[A](initialValue: A) = new LockablePointer[A](initialValue)
	
	/**
	  * @tparam A Type of values held within this pointer, when defined
	  * @return An empty pointer that may contain values of the specified type
	  */
	def empty[A]() = apply[Option[A]](None)
}

/**
  * This mutable pointer class allows one to flag it as "locked",
  * after which it can't be modified anymore, and is considered static in terms of change-listener handling.
  * @author Mikko Hilpinen
  * @since 26.7.2023, v2.2
  */
class LockablePointer[A](initialValue: A) extends AbstractChanging[A] with Pointer[A]
{
	// ATTRIBUTES   -------------------------
	
	private var _value = initialValue
	private var _locked = false
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Whether this pointer has been locked and won't change anymore
	  */
	def locked = _locked
	
	
	// IMPLEMENTED  -------------------------
	
	override def value: A = _value
	@throws[IllegalStateException]("If this pointer has been locked")
	override def value_=(newValue: A): Unit = {
		if (_locked)
			throw new IllegalStateException("This pointer has been locked")
		else
			_set(newValue)
	}
	
	override def isChanging: Boolean = !_locked
	
	
	// OTHER    ---------------------------
	
	/**
	  * Locks this pointer, so that it can't be changed anymore
	  */
	def lock() = {
		_locked = true
		// Discards all listeners, since they won't be informed about anything anymore
		clearListeners()
	}
	
	/**
	  * Attempts to modify the value of this pointer.
	  * Won't modify the value if this pointer has been locked.
	  * @param value A value that should be set to this pointer, if possible
	  * @return Whether that value is now the current value of this pointer.
	  *         I.e. true if this pointer was not locked or contained an identical value already.
	  */
	def trySet(value: A) = {
		if (locked)
			value == _value
		else {
			_set(value)
			true
		}
	}
	
	// Expects locking status to be checked
	private def _set(newValue: A) = {
		val oldValue = _value
		_value = newValue
		fireEventIfNecessary(oldValue, newValue).foreach { _() }
	}
}