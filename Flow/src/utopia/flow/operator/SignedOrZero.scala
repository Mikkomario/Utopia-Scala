package utopia.flow.operator

import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * A common trait for items which can be either positive or negative, or zero
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait SignedOrZero[+Repr] extends Any with Signed[Repr] with Zeroable[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A zero value copy of this item
	  */
	protected def zero: Repr
	
	
	// COMPUTED    ----------------------
	
	/**
	  * @return Whether this item is positive or zero (<=0)
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
	  * @return A positive or zero value copy of this item
	  */
	def positiveOrZero = if (isPositive) repr else zero
	/**
	  * @return A negative or zero value of this item
	  */
	def negativeOrZero = if (isPositive) zero else repr
	
	
	// IMPLEMENTED  ----------------------
	
	override def isNegative = !isPositiveOrZero
}
