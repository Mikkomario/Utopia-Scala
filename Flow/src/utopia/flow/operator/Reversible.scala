package utopia.flow.operator

import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * A common trait for instances which can be reversed (support the unary - -operator)
  * @author Mikko Hilpinen
  * @since 20.9.2021, v1.12
  */
trait Reversible[+Repr] extends Any
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return 'This' instance
	  */
	def self: Repr
	
	/**
	  * @return A reversed copy of this item
	  */
	def unary_- : Repr
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param sign A sign
	  * @return Kept or reversed copy of this item, depending on the sign
	  */
	def *(sign: Sign) = sign match {
		case Positive => self
		case Negative => -this
	}
}
