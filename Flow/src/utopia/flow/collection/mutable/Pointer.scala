package utopia.flow.collection.mutable

import utopia.flow.datastructure.mutable.PointerWithEvents

object Pointer
{
    /**
     * Creates a new pointer for value
     */
    def apply[A](value: A) = new Pointer(value)
	
	/**
	  * Creates a new empty pointer that contains an option
	  * @tparam A Type of values in this pointer, when specified
	  * @return A new empty option pointer
	  */
	def option[A]() = new Pointer[Option[A]](None)
	
	/**
	  * Creates a new pointer with events
	  * @param value The initial value for the pointer
	  * @tparam A The type of the contained item
	  * @return A new pointer with events
	  */
	def withEvents[A](value: A) = new PointerWithEvents(value)
}

/**
* This is a simple structure for holding a single mutable value
* @author Mikko Hilpinen
* @since 23.3.2019
**/
class Pointer[A](override var value: A) extends Settable[A]
{
	/**
	  * The current value in this pointer
	  */
	@deprecated("Please use .value instead", "v1.9")
	def get = value
	
	/**
	  * Updates the value in this pointer
	  */
	@deprecated("Please assign directly to the value instead", "v1.9")
	def set(newVal: A) = value = newVal
}