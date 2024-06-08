package utopia.vault.sql

import utopia.flow.collection.immutable.Pair

/**
 * Common trait for order directions
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
sealed trait OrderDirection
{
	/**
	 * @return An sql representation of this order direction
	 */
	def toSql: String
}

object OrderDirection
{
	/**
	 * Ascending order direction where smaller values come first
	 */
	case object Ascending extends OrderDirection
	{
		override def toSql = "ASC"
	}
	
	/**
	 * Descending order direction where larger values come first
	 */
	case object Descending extends OrderDirection
	{
		override def toSql = "DESC"
	}
	
	/**
	 * All possible order direction values
	 */
	val values = Pair(Ascending, Descending)
}
