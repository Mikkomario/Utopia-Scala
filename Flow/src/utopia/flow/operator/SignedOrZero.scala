package utopia.flow.operator

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.SignOrZero.Neutral

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
	def isPositiveOrZero = sign.isPositive || isZero
	/**
	  * @return Whether this item is negative or zero (<=0)
	  */
	def isNegativeOrZero = sign.isNegative || isZero
	
	/**
	  * @return Sign of this item. None if zero.
	  */
	@deprecated("Please use .sign.binary instead", "v2.2")
	def signOption: Option[Sign] = sign.binary
	
	/**
	  * @return A copy of this item that's at least zero.
	  *         I.e. if this item is below zero, returns zero. Otherwise returns this.
	  */
	def minZero = if (sign.isNegative) zero else self
	/**
	  * @return A copy of this item that's at most zero.
	  *         I.e. if this item is above zero, returns zero. Otherwise returns this.
	  */
	def maxZero = if (sign.isPositive) zero else self
	
	/**
	  * @return A positive or zero value copy of this item
	  */
	@deprecated("Please use minZero instead", "v2.0")
	def positiveOrZero = minZero
	/**
	  * @return A negative or zero value of this item
	  */
	@deprecated("Please use maxZero instead", "v2.0")
	def negativeOrZero = maxZero
	
	
	// IMPLEMENTED  ----------------------
	
	override def isZero: Boolean = sign == Neutral
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param mod A scaling modifier (-1, 0 or 1)
	  * @return A scaled copy of this item
	  */
	def *(mod: SignOrZero) = mod match {
		case Positive => self
		case Negative => -this
		case Neutral => zero
	}
}
