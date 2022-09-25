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
	  * @return True if this item is not positive. False otherwise.
	  */
	def nonPositive = !isPositive
	/**
	  * @return True if this item is not negative. False otherwise.
	  */
	def nonNegative = !isNegative
	
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
	
	/**
	  * @return Some(this) if not positive, None otherwise
	  */
	def notPositive = if (!isPositive) Some(repr) else None
	/**
	  * @return Some(this) if not negative, None otherwise
	  */
	def notNegative = if (!isNegative) Some(repr) else None
	
	
	// OTHER    --------------------------
	
	/**
	  * @param sign A sign
	  * @return Whether this instance is of that sign. NB: Zero returns false for both signs.
	  */
	def is(sign: Sign) = sign match {
		case Positive => isPositive
		case Negative => isNegative
	}
	
	/**
	  * @param default Value to return if this item is not positive
	  * @tparam B Type of default item
	  * @return This item if positive, otherwise the specified default
	  */
	def positiveOrElse[B >: Repr](default: => B) = if (isPositive) repr else default
	/**
	  * @param default Value to return if this item is not negative
	  * @tparam B Type of default item
	  * @return This item if negative, otherwise the specified default
	  */
	def negativeOrElse[B >: Repr](default: => B) = if (isNegative) repr else default
	
	/**
	  * @param f A mapping function to apply if this item is positive
	  * @tparam B Type of mapping result
	  * @return Mapped copy of this item if this was positive, otherwise this item as is
	  */
	def mapIfPositive[B >: Repr](f: Repr => B) = if (isPositive) f(repr) else repr
	/**
	  * @param f A mapping function to apply if this item is negative
	  * @tparam B Type of mapping result
	  * @return Mapped copy of this item if this was negative, otherwise this item as is
	  */
	def mapIfNegative[B >: Repr](f: Repr => B) = if (isNegative) f(repr) else repr
}
