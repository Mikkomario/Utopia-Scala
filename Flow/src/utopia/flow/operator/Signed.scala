package utopia.flow.operator

import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * A common trait for items which can be either positive or negative, but not necessarily zero
  * @author Mikko Hilpinen
  * @since 21.9.2021, v1.12
  */
trait Signed[+Repr] extends Any with Reversible[Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Whether this item is positive (>0)
	  */
	def isPositive: Boolean
	/**
	  * @return Whether this item is negative (<0)
	  */
	def isNegative: Boolean
	
	
	// COMPUTED    ----------------------
	
	/**
	  * @return Sign of this item. Returns Positive for zero items.
	  */
	def sign: Sign = if (isNegative) Negative else Positive
	
	/**
	  * @return Absolute value of this item
	  */
	def abs = if (isNegative) -this else repr
	/**
	  * @return Negative absolute value of this item
	  */
	def negativeAbs = if (isPositive) -this else repr
	
	/**
	  * @return Some(this) if positive, None otherwise
	  */
	def ifPositive = if (isPositive) Some(repr) else None
	/**
	  * @return Some(this) if negative, None otherwise
	  */
	def ifNegative = if (isNegative) Some(repr) else None
	
	
	// OTHER    --------------------------
	
	/**
	  * @param sign A sign
	  * @return Whether this instance is of that sign. NB: Zero returns false for both signs.
	  */
	def is(sign: Sign) = sign match
	{
		case Positive => isPositive
		case Negative => isNegative
	}
}
