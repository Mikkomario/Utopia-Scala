package utopia.genesis.util

/**
  * Common traits for items that can be positive or negative
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
@deprecated("Please use Signed from Flow instead", "v2.6")
trait Signed[+Repr] extends Scalable[Repr]
{
	// ABSTRACT	-------------------
	
	/**
	  * @return Whether this item is positive (>= 0)
	  */
	def isPositive: Boolean
	
	/**
	  * @return A copy of this item with 0 value
	  */
	protected def zero: Repr
	
	
	// OTHER	-------------------
	
	/**
	  * @return This item if positive, otherwise 0
	  */
	def positive = if (isPositive) repr else zero
	
	/**
	  * @return The absolute value of this item, always positive
	  */
	def abs = if (isPositive) repr else -this
}
