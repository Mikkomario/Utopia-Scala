package utopia.flow.generic.model.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.Binary
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

/**
 * An enumeration for different operations / operators supported for interaction between two values.
 * @author Mikko Hilpinen
 * @since 10.11.2026, v2.9
 */
sealed trait ValueOperation2
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return Code that represents this operator
	 */
	def code: String
	
	
	// IMPLEMENTED  ------------------------
	
	override def toString = code
}

object ValueOperation2
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * All currently supported operations
	 */
	val values = Pair[ValueOperation2](Plus, Minus)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param code An operation code. E.g. "+".
	 * @return An operation that matches the specified code.
	 *         None if no (currently supported) operation matched that code.
	 */
	def findFor(code: String) = values.find { _.code == code }
	
	/**
	 * @param sign A sign as an operator (+ or -)
	 * @return An operator that matches the specified sign
	 */
	def apply(sign: Sign): CombineValuesOperation = sign match {
		case Positive => Plus
		case Negative => Minus
	}
	
	
	// NESTED   ----------------------------
	
	sealed trait CombineValuesOperation extends ValueOperation2 with Binary[CombineValuesOperation]
	{
		// ABSTRACT ------------------------
		
		/**
		 * @return Sign applied to the combined value
		 */
		def sign: Sign
		
		
		// IMPLEMENTED  --------------------
		
		override def self: CombineValuesOperation = this
		
		override def compareTo(o: CombineValuesOperation): Int = sign.compareTo(o.sign)
	}
	
	
	// VALUES   ----------------------------
	
	/**
	 * Combines the value of two values
	 */
	case object Plus extends CombineValuesOperation
	{
		override val code: String = "+"
		override val sign: Sign = Positive
		
		override def unary_- : CombineValuesOperation = Minus
	}
	/**
	 * Subtracts a value from another
	 */
	case object Minus extends CombineValuesOperation
	{
		override val code: String = "-"
		override val sign: Sign = Negative

		override def unary_- : CombineValuesOperation = Plus
	}
}
