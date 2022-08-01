package utopia.flow.util

/**
  * A common trait for context items that are usable in a specific scope
  * @author Mikko Hilpinen
  * @since 1.8.2022, v1.16
  */
trait ScopeUsable[+Repr]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return A representation of this item
	  */
	def repr: Repr
	
	
	// OTHER	----------------------------
	
	/**
	  * Opens a new "scope" that accepts this item
	  * @param f A function called for this item
	  * @tparam R Function result type
	  * @return Function result
	  */
	def use[R](f: Repr => R) = f(repr)
}
