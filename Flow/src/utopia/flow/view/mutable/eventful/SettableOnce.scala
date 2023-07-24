package utopia.flow.view.mutable.eventful

import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.AbstractChanging

object SettableOnce
{
	/**
	  * @tparam A Type of set item
	  * @return A new pointer that may only be set once
	  */
	def apply[A]() = new SettableOnce[A]()
	
	/**
	  * @param value A preset value
	  * @tparam A Type of the specified value
	  * @return A pointer that has been set and can't be modified anymore
	  */
	def set[A](value: A) = {
		val pointer = new SettableOnce[A]()
		pointer.set(value)
		pointer
	}
}

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
	
	/**
	  * @return The value set to this pointer
	  * @throws IllegalStateException If this pointer hasn't been set
	  */
	@throws[IllegalStateException]("No value has been set yet")
	def get = value.getOrElse { throw new IllegalStateException("Called get before the value was set") }
	
	
	// IMPLEMENTED  -------------------------
	
	override def isChanging = _value.isEmpty
	
	override def value = _value
	@throws[IllegalStateException]("If this pointer has already been set")
	override def value_=(newValue: Option[A]) = {
		if (_value.exists { v => !newValue.contains(v) })
			throw new IllegalStateException("SettableOnce.value may only be defined once")
		else if (newValue.isDefined) {
			_value = newValue
			fireChangeEvent(None)
			clearListeners()
		}
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
	
	/**
	  * Calls the specified function once this pointer has been set.
	  * If already set, calls the function immediately.
	  * @param f A function to call once this pointer has been set.
	  * @tparam U Arbitrary function result type.
	  */
	def onceSet[U](f: A => U) = addListenerAndSimulateEvent(None) { e =>
		e.newValue match {
			// Case: Set event => Calls the function and ends listening
			case Some(value) =>
				f(value)
				Detach
			// Case: Reset event (not applicable)
			case None => Continue
		}
	}
}
