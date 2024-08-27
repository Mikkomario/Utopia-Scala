package utopia.flow.view.mutable.eventful

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.PointerFactory
import utopia.flow.view.template.eventful.{AbstractMayStopChanging, Changing, ChangingWrapper}

import scala.annotation.unused
import scala.language.implicitConversions
import scala.util.Try

object LockablePointer
{
	// COMPUTED --------------------------
	
	/**
	  * @param log Logging implementation for handling failures in change-event -handling
	  * @return Factory for constructing lockable pointers
	  */
	def factory(implicit log: Logger): PointerFactory[LockablePointer] = new LockablePointerFactory()
	
	
	// IMPLICIT --------------------------
	
	implicit def objectToFactory(@unused o: LockablePointer.type)
	                            (implicit log: Logger): PointerFactory[LockablePointer] = factory
	
	
	// OTHER    --------------------------
	
	/**
	  * @param initialValue Initial value to assign to this pointer
	  * @param log Logging implementation for handling failures in change-event -handling
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	def apply[A](initialValue: A)(implicit log: Logger) = new LockablePointer[A](initialValue)
	
	
	// NESTED   ---------------------------
	
	private class LockablePointerFactory(implicit log: Logger) extends PointerFactory[LockablePointer]
	{
		override def apply[A](initialValue: A): LockablePointer[A] = LockablePointer(initialValue)
	}
}

/**
  * This mutable pointer class allows one to flag it as "locked",
  * after which it can't be modified anymore, and is considered static in terms of change-listener handling.
  * @author Mikko Hilpinen
  * @since 26.7.2023, v2.2
  */
class LockablePointer[A](initialValue: A)(implicit log: Logger)
	extends AbstractMayStopChanging[A] with Lockable[A] with EventfulPointer[A]
{
	// ATTRIBUTES   -------------------------
	
	private var _value = initialValue
	private var _locked = false
	
	override lazy val readOnly: Changing[A] = ChangingWrapper(this)
	
	
	// IMPLEMENTED  -------------------------
	
	override def value: A = _value
	@throws[IllegalStateException]("If this pointer has been locked")
	override def value_=(newValue: A): Unit = {
		if (_locked)
			throw new IllegalStateException("This pointer has been locked")
		else
			_set(newValue)
	}
	
	override def locked = _locked
	
	override def lock() = {
		_locked = true
		// Discards all listeners, since they won't be informed about anything anymore
		declareChangingStopped()
	}
	
	
	// OTHER    ---------------------------
	
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
		fireEventIfNecessary(oldValue, newValue).foreach { effect => Try { effect() }.logFailure }
	}
}
