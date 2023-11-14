package utopia.flow.operator.sign

import utopia.flow.operator.MayBeZero
import utopia.flow.operator.sign.SignOrZero.Neutral

/**
  * A common trait for items which can be either positive or negative, or zero
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait SignedOrZero[+Repr] extends Any with Signed[Repr] with MayBeZero[Repr]
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
	
	
	// IMPLEMENTED  ----------------------
	
	override def isZero: Boolean = sign == Neutral
}
