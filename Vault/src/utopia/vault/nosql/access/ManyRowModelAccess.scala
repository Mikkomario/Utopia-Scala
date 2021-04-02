package utopia.vault.nosql.access

import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Condition, Count, Exists, OrderBy, Select, Where}

/**
  * Used for accessing multiple models at once, each model occupying exactly one row
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.6
  */
trait ManyRowModelAccess[+A] extends RowModelAccess[A, Vector[A]] with ManyModelAccess[A]
{
	// COMPUTED -----------------------------
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return Number of items accessible from this accessor
	  */
	def size(implicit connection: Connection) = connection(Count(target) + globalCondition.map { Where(_) })
		.firstValue.getInt
	
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
	
	
	// OTHER    ---------------------------
	
	/**
	 * Reads values from individual columns for each accessible row
	 * @param column Column to read
	 * @param additionalCondition Additional condition to apply (optional)
	 * @param order Order to apply (optional)
	 * @param connection DB Connection (implicit)
	 * @return Read values
	 */
	protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                         order: Option[OrderBy] = None)(implicit connection: Connection) =
		connection(Select(target, column) + mergeCondition(additionalCondition).map { Where(_) } + order).rowValues
	
	/**
	 * Collects values of a column for all accessible rows
	 * @param column Column that is read
	 * @param order Ordering applied (optional)
	 * @param connection DB Connection (implicit)
	 * @return Read column values
	 */
	def pullColumn(column: Column, order: Option[OrderBy] = None)(implicit connection: Connection) =
		readColumn(column, order = order)
	
	/**
	 * Collects values of a column / attribute for all accessible rows
	 * @param attributeName Name of the attribute to read
	 * @param order Ordering applied (optional)
	 * @param connection DB Connection (implicit)
	 * @return Read column values
	 */
	def pullAttribute(attributeName: String, order: Option[OrderBy] = None)(implicit connection: Connection) =
		pullColumn(table(attributeName), order)
	
	/**
	 * Collects column values of a specific subset of rows
	 * @param column Column to read
	 * @param condition Search condition to apply
	 * @param order Ordering to apply (optional)
	 * @param connection DB Connection (implicit)
	 * @return Read column values
	 */
	def findColumn(column: Column, condition: Condition, order: Option[OrderBy] = None)
	              (implicit connection: Connection) = readColumn(column, Some(condition), order)
	
	/**
	 * Collects column / attribute values of a specific subset of rows
	 * @param attributeName Name of the targeted attribute
	 * @param condition Search condition to apply
	 * @param order Ordering to apply (optional)
	 * @param connection DB Connection (implicit)
	 * @return Read column / attribute values
	 */
	def findAttribute(attributeName: String, condition: Condition, order: Option[OrderBy] = None)
	                 (implicit connection: Connection) =
		findColumn(table(attributeName), condition, order)
}
