package utopia.flow.datastructure.mutable

/**
  * This trait is extended by classes that reseble pointers
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1+
  * @tparam A The type of item pointed to
  */
@deprecated("Please use the Settable -trait instead", "v1.9")
trait PointerLike[A]
{
	// ABSTRACT    -------------------------
	
	/**
	  * The current value in this pointer
	  */
	def get: A
	
	/**
	  * Updates the value in this pointer
	  */
	def set(newVal: A): Unit
	
	
	// IMPLEMENTED    ----------------------
	
	override def toString = get.toString
	
	
	// OTHER    ------------------------
	
	/**
	  * Whether this pointer points to the specified value
	  */
	def contains(item: Any) = get == item
	
	/**
	  * Updates the value in this pointer
	  * @param f A function for changing this pointer's value
	  */
	def update(f: A => A) = set(f(get))
}
