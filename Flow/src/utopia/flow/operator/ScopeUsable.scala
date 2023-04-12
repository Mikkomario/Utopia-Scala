package utopia.flow.operator

/**
  * A common trait for context items that are usable in a specific scope
  * @author Mikko Hilpinen
  * @since 1.8.2022, v1.16
  */
trait ScopeUsable[+Repr] extends Any
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return A representation of this item
	  */
	def self: Repr
	
	
	// OTHER	----------------------------
	
	/**
	  * Opens a new "scope" that accepts this item
	  * @param f A function called for this item
	  * @tparam R Function result type
	  * @return Function result
	  */
	def use[R](f: Repr => R) = f(self)
}
