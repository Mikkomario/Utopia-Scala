package utopia.flow.operator.sign

import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.SignOrZero.Neutral

/**
  * Common trait for items that may have a sign, but where this sign is not always known for certain.
  * @author Mikko Hilpinen
  * @since 19.8.2023, v2.2
  */
trait HasUncertainSign
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Sign of this item (may be uncertain)
	  */
	def sign: UncertainSign
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Whether this item is positive (may be uncertain)
	  */
	def isPositive = is(Positive)
	/**
	  * @return Whether this item is negative (may be uncertain)
	  */
	def isNegative = is(Negative)
	/**
	  * @return Whether this item is zero (may be uncertain)
	  */
	def isZero = is(Neutral)
	/**
	  * @return Whether this item is not zero (may be uncertain)
	  */
	def nonZero = !isZero
	
	/**
	  * @return Whether it is known for certain that this item is positive
	  */
	def isCertainlyPositive = isCertainly(Positive)
	/**
	  * @return Whether it is known for certain that this item is negative
	  */
	def isCertainlyNegative = isCertainly(Negative)
	/**
	  * @return Whether it is known for certain that this item is zero
	  */
	def isCertainlyZero = isCertainly(Neutral)
	
	/**
	  * @return Whether it is known for certain that this item is not positive
	  */
	def isCertainlyNotPositive = isPositive.isCertainlyFalse
	/**
	  * @return Whether it is known for certain that this item is not negative
	  */
	def isCertainlyNotNegative = isNegative.isCertainlyFalse
	/**
	  * @return Whether it is known for certain that this item is not zero
	  */
	def isCertainlyNotZero = isZero.isCertainlyFalse
	
	/**
	  * @return Whether there is a possibility that this item is positive
	  */
	def mayBePositive = mayBe(Positive)
	/**
	  * @return Whether there is a possibility that this item is negative
	  */
	def mayBeNegative = mayBe(Negative)
	/**
	  * @return Whether there is a possibility that this item is zero
	  */
	def mayBeZero = mayBe(Neutral)
	
	/**
	  * @return Whether it is possible that this item is not positive
	  */
	def mayBeNonPositive = mightNotBe(Positive)
	/**
	  * @return Whether it is possible that this item is not negative
	  */
	def mayBeNonNegative = mightNotBe(Negative)
	/**
	  * @return Whether it is possible that this item is not zero
	  */
	def mayBeNonZero = mightNotBe(Neutral)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param sign A sign
	  * @return Whether this item has that sign (may be uncertain)
	  */
	def is(sign: SignOrZero) = this.sign == sign
	/**
	  * @param sign A sign
	  * @return Whether it is known for certain that this item has that sign
	  */
	def isCertainly(sign: SignOrZero) = is(sign).isCertainlyTrue
	
	/**
	  * @param sign A sign
	  * @return Whether it is possible that this item has that sign
	  */
	def mayBe(sign: SignOrZero) = this.sign.mayBe(sign)
	/**
	  * @param sign A sign
	  * @return Whether it is possible that this item doesn't have that sign
	  */
	def mightNotBe(sign: SignOrZero) = !isCertainly(sign)
}
