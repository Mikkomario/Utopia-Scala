package utopia.reflection.component.context

/**
  * A common trait for context items that are usable in a specific scope
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
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
	  * @param f A function called for this item
	  * @tparam R Function result
	  */
	def use[R](f: Repr => R) = f(repr)
}
