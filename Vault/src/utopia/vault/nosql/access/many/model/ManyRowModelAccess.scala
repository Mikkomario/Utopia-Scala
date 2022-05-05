package utopia.vault.nosql.access.many.model

import utopia.vault.database.Connection
import utopia.vault.nosql.view.RowFactoryView
import utopia.vault.sql.{Count, OrderBy, Where}

/**
  * Used for accessing multiple models at once, each model occupying exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait ManyRowModelAccess[+A] extends ManyModelAccess[A] with RowFactoryView[A]
{
	// COMPUTED -----------------------------
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Number of items accessible from this accessor
	  */
	def size(implicit connection: Connection) = connection(Count(target) + globalCondition.map { Where(_) })
		.firstValue.getInt
	
	
	// OTHER    -----------------------------
	
	/**
	 * Reads the first n accessible items
	 * @param order Ordering to use
	 * @param maxSize Maximum number of items returned
	 * @param connection Implicit DB Connection
	 * @return The first n accessible items
	 */
	def take(order: OrderBy, maxSize: Int)(implicit connection: Connection) =
		factory.take(maxSize, order, globalCondition)
}
