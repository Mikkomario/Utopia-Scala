package utopia.vault.sql

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.{BinarySigned, Sign}

/**
 * Common trait for order directions
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
sealed trait OrderDirection extends BinarySigned[OrderDirection]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return A SQL representation of this order direction
	 */
	def toSql: String
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: OrderDirection = this
}

object OrderDirection
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * All possible order direction values
	  */
	lazy val values = Pair(Ascending, Descending)
	
	
	// VALUES   -------------------------
	
	/**
	 * Ascending order direction where smaller values come first
	 */
	case object Ascending extends OrderDirection
	{
		override val toSql = "ASC"
		override val sign: Sign = Positive
		
		override def unary_- : OrderDirection = Descending
	}
	/**
	 * Descending order direction where larger values come first
	 */
	case object Descending extends OrderDirection
	{
		override val toSql = "DESC"
		override val sign: Sign = Negative
		
		override def unary_- : OrderDirection = Ascending
	}
}
