package utopia.flow.operator

/**
  * A common trait for items for which it is possible to recognize whether it is zero or not
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait MayBeZero[+Repr] extends Any
{
	// ABSTRACT --------------------------
	
	/**
	  * @return A version of this item that has a zero value
	  */
	def zero: Repr
	
	/**
	  * @return 'This' item
	  */
	def self: Repr
	
	/**
	  * @return Whether this item is of zero length
	  */
	def isZero: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Whether this item is not of zero length
	  */
	def nonZero = !isZero
	/**
	  * @return This item if not zero. None otherwise.
	  */
	def notZero = if (isZero) None else Some(self)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param default A value to return if this item is zero
	  * @tparam B Type of the default value
	  * @return This item if not zero, otherwise the default value
	  */
	def nonZeroOrElse[B >: Repr](default: => B) = if (isZero) default else self
	/**
	  * Maps this item, but only if not of value zero
	  * @param f A mapping function to apply to non-zero items
	  * @tparam B Type of map function result
	  * @return This item if zero, otherwise the mapping function result
	  */
	def mapIfNotZero[B >: Repr](f: Repr => B) = if (isZero) self else f(self)
}
