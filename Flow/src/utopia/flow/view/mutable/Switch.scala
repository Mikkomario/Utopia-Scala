package utopia.flow.view.mutable

import scala.language.implicitConversions

object Switch
{
	// IMPLICIT -------------------------
	
	/**
	  * (Implicitly) wraps a boolean-based pointer and converts it to a Switch
	  * @param pointer Pointer to wrap
	  * @return A Switch wrapping the specified pointer.
	  *         If the pointer was already an instance of Switch, returns it as is.
	  */
	implicit def wrap(pointer: Pointer[Boolean]): Switch = pointer match {
		case s: Switch => s
		case p => new SwitchWrapper(p)
	}
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a new Switch
	  * @param initialState Initial state to assign. Default = false.
	  * @return A new switch
	  */
	def apply(initialState: Boolean = false): Switch = new _Switch(initialState)
	
	
	// NESTED   -------------------------
	
	private class SwitchWrapper(pointer: Pointer[Boolean]) extends Switch
	{
		override def value: Boolean = pointer.value
		override def value_=(newValue: Boolean): Unit = pointer.value = newValue
	}
	
	private class _Switch(initialState: Boolean) extends Switch
	{
		override var value: Boolean = initialState
	}
}

/**
  * Common trait for items which may be set or reset at will
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
trait Switch extends Settable with Resettable with Pointer[Boolean]
{
	// IMPLEMENTED  -------------------
	
	override def isSet: Boolean = value
	
	override def set(): Boolean = mutate { !_ -> true }
	override def reset(): Boolean = mutate { _ -> false }
	
	
	// OTHER    -----------------------
	
	/**
	  * If this item has been set, resets it. Otherwise, sets it.
	  */
	def switch() = value = !value
}