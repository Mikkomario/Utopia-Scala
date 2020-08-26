package utopia.vault.nosql.access

import utopia.vault.database.Connection
import utopia.vault.sql.{Count, Exists, Select, Where}

/**
  * Used for accessing multiple models at once, each model occupying exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait ManyRowModelAccess[+A] extends RowModelAccess[A, Vector[A]] with ManyModelAccess[A]
{
	/**
	  * @param connection DB Connection (implicit)
	  * @return Number of items accessible from this accessor
	  */
	def size(implicit connection: Connection) = connection(Count(target) + globalCondition.map { Where(_) })
		.firstValue.getInt
		// connection(Select.nothing(target) +
		// globalCondition.map { Where(_) }).rows.size
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there are any items accessible from this accessor
	  */
	def nonEmpty(implicit connection: Connection) = globalCondition match
	{
		case Some(condition) => Exists(target, condition)
		case None => Exists.any(target)
	}
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Whether there are no items accessible from this accessor
	  */
	def isEmpty(implicit connection: Connection) = !nonEmpty
}
