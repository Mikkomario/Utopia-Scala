package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.sql.{Count, Where}

/**
  * Used for accessing multiple models at once, each model occupying exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait ManyRowModelAccess[+A] extends RowModelAccess[A, Vector[A], Vector[Value]] with ManyModelAccess[A]
{
	// COMPUTED -----------------------------
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Number of items accessible from this accessor
	  */
	def size(implicit connection: Connection) = connection(Count(target) + globalCondition.map { Where(_) })
		.firstValue.getInt
}
