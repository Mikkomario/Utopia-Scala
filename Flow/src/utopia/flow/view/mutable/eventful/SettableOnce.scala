package utopia.flow.view.mutable.eventful

import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.AbstractChanging

/**
  * A container that works like a Promise, in the sense that it can only be set once.
  * Supports ChangeEvents.
  * @author Mikko Hilpinen
  * @since 16.11.2022, v2.0
  */
class SettableOnce[A]() extends AbstractChanging[Option[A]] with Pointer[Option[A]]
{
	// ATTRIBUTES   -------------------------
	
	private var _value: Option[A] = None
	
	/**
	  * @return Future that resolves once this pointer is set
	  */
	lazy val future = findMapFuture { a => a }
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Whether this pointer has been set and is now immutable
	  */
	def isCompleted = _value.isDefined
	/**
	  * @return Whether the value of this pointer has yet to be defined
	  */
	def isEmpty = !isCompleted
	
	
	// IMPLEMENTED  -------------------------
	
	override def isChanging = _value.isEmpty
	
	override def value = _value
	@throws[IllegalStateException]("If this pointer has already been set")
	override def value_=(newValue: Option[A]) = {
		if (_value.exists { v => !newValue.contains(v) })
			throw new IllegalStateException("SettableOnce.value may only be defined once")
		else
			_value = newValue
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Specifies the value in this pointer
	  * @param value Value for this pointer to hold
	  * @throws IllegalStateException If this pointer has already been set
	  */
	@throws[IllegalStateException]("If this pointer has already been set")
	def set(value: A) = this.value = Some(value)
	/**
	  * Specifies the value in this pointer, unless specified already
	  * @param value Value for this pointer to hold (call-by-name, only called if this pointer is empty)
	  * @return Whether this pointer was set.
	  *         False if this pointer had already been set.
	  */
	def trySet(value: => A) = {
		if (isCompleted)
			false
		else {
			this.value = Some(value)
			true
		}
	}
}
