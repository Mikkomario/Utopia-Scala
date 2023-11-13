package utopia.flow.operator.sign

/**
  * A common trait for items which can be either positive or negative, but not necessarily zero
  * @author Mikko Hilpinen
  * @since 21.9.2021, v1.12
  */
trait HasSign extends Any
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The sign of this item
	  */
	def sign: SignOrZero
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return Whether this item is positive (>0)
	  */
	def isPositive: Boolean = sign.isPositive
	/**
	  * @return Whether this item is negative (<0)
	  */
	def isNegative: Boolean = sign.isNegative
	/**
	  * @return Whether this item is negative or zero
	  */
	def isNotPositive: Boolean = sign.isNotPositive
	/**
	  * @return Whether this item is positive or zero
	  */
	def isNotNegative: Boolean = sign.isNotNegative
	
	
	// OTHER    --------------------------
	
	/**
	  * @param sign A sign or zero / neutral
	  * @return Whether this instance is of that sign.
	  */
	def is(sign: SignOrZero) = this.sign == sign
	/**
	  * @param sign A sign or zero / neutral
	  * @return Whether this item is not of that sign
	  */
	def isNot(sign: SignOrZero) = this.sign != sign
}
