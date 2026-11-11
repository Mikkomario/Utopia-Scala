package utopia.flow.generic.model.enumeration

import utopia.flow.collection.immutable.Single

/**
 * An enumeration for different unary operations that may be applied to generic values
 * @author Mikko Hilpinen
 * @since 10.11.2026, v2.9
 */
sealed trait ValueOperation1
{
	// ABSTRACT ---------------------
	
	/**
	 * @return A code that represents this operation
	 */
	def code: String
	
	
	// IMPLEMENTED  -----------------
	
	override def toString: String = code
}

object ValueOperation1
{
	// ATTRIBUTES   -----------------
	
	/**
	 * All currently supported unary operations
	 */
	lazy val values = Single[ValueOperation1](Negate)
	
	
	// VALUES   ---------------------
	
	/**
	 * An operation that negates or inverts the value in question
	 */
	case object Negate extends ValueOperation1
	{
		override def code: String = "-"
	}
}