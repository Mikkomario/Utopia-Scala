package utopia.flow.view.mutable.eventful

import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.AbstractChanging

/**
  * A pointer that, besides the initial value, may only be set once
  * @author Mikko Hilpinen
  * @since 22.12.2022, v2.0
  * @tparam A Type of value held within this pointer
  */
class MutableOnce[A](initialValue: A) extends AbstractChanging[A] with Pointer[A]
{
	// ATTRIBUTES   -----------------------
	
	private val _setFlag = Flag()
	
	private var _value = initialValue
	
	/**
	  * A future that resolves when this item is mutated
	  */
	lazy val future = _setFlag.findMapFuture { if (_) Some(_value) else None }
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return A flag that becomes set when this item is mutated
	  */
	def flag = _setFlag.view
	
	
	// IMPLEMENTED  -----------------------
	
	override def isChanging = _setFlag.isNotSet
	
	override def value = _value
	override def value_=(newValue: A) = {
		if (_setFlag.isSet)
			throw new IllegalStateException("This pointer has already been set")
		else {
			if (_value != newValue) {
				val oldValue = _value
				_value = newValue
				_setFlag.set()
				fireChangeEvent(oldValue)
			}
			else {
				_value = newValue
				_setFlag.set()
			}
		}
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * Assigns a new value to this item
	  * @param newValue New value to assign
	  * @throws IllegalStateException If this pointer was already set before
	  */
	@throws[IllegalStateException]("This pointer has already been set once")
	def set(newValue: A) = value = newValue
	
	/**
	  * Attempts to assign a new value to this item.
	  * Will not assign the value if this pointer has already been set before.
	  * @param newValue A new value to assign
	  * @return Whether that value was assigned
	  */
	def trySet(newValue: => A) = {
		if (isChanging) {
			value = newValue
			true
		}
		else
			false
	}
}