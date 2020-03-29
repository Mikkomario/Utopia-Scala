package utopia.flow.datastructure.mutable

object Pointer
{
    /**
     * Creates a new pointer for value
     */
    def apply[A](value: A) = new Pointer(value)
	
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
class Pointer[A](var value: A) extends PointerLike[A]
{
	/**
	  * The current value in this pointer
	  */
	def get = value
	
	/**
	  * Updates the value in this pointer
	  */
	def set(newVal: A) = value = newVal
}