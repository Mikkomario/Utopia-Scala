package utopia.flow.operator

/**
  * A common trait for items which can be either positive or negative
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait Signed[+Repr] extends Any with Reversible[Repr] with LengthLike[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Whether this item is positive (>0)
	  */
	def isPositive: Boolean
	
	/**
	  * @return A zero value copy of this item
	  */
	protected def zero: Repr
	
	
	// COMPUTED    ----------------------
	
	/**
	  * @return Whether this item is negative (<0)
	  */
	def isNegative = !isPositiveOrZero
	
	/**
	  * @return Whether this item is positive or zero (<=0)
	  */
	def isPositiveOrZero = isPositive || isZero
	/**
	  * @return Whether this item is negative or zero (<=0)
	  */
	def isNegativeOrZero = !isPositive
	
	/**
	  * @return A positive or zero value copy of this item
	  */
	def positive = if (isPositive) repr else zero
	/**
	  * @return A negative or zero value of this item
	  */
	def negative = if (isPositive) zero else repr
	
	/**
	  * @return Absolute value of this item
	  */
	def abs = if (isPositive) repr else -this
}
