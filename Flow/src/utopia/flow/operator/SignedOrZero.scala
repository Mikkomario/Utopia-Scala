package utopia.flow.operator

import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * A common trait for items which can be either positive or negative, or zero
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait SignedOrZero[+Repr] extends Any with Signed[Repr] with CanBeZero[Repr]
{
	// COMPUTED    ----------------------
	
	/**
	  * @return Whether this item is positive or zero (>=0)
	  */
	def isPositiveOrZero = isPositive || isZero
	/**
	  * @return Whether this item is negative or zero (<=0)
	  */
	def isNegativeOrZero = !isPositive
	
	/**
	  * @return Sign of this item. None if zero.
	  */
	def signOption: Option[Sign] = if (isPositive) Some(Positive) else if (isZero) None else Some(Negative)
	
	/**
	  * @return A copy of this item that's at least zero.
	  *         I.e. if this item is below zero, returns zero. Otherwise returns this.
	  */
	def minZero = if (isPositive) self else zero
	/**
	  * @return A copy of this item that's at most zero.
	  *         I.e. if this item is above zero, returns zero. Otherwise returns this.
	  */
	def maxZero = if (isPositive) zero else self
	
	/**
	  * @return A positive or zero value copy of this item
	  */
	@deprecated("Please use minZero instead", "v2.0")
	def positiveOrZero = if (isPositive) self else zero
	/**
	  * @return A negative or zero value of this item
	  */
	@deprecated("Please use maxZero instead", "v2.0")
	def negativeOrZero = if (isPositive) zero else self
	
	
	// IMPLEMENTED  ----------------------
	
	override def isNegative = !isPositiveOrZero
}
