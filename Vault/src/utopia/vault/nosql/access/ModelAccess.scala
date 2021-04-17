package utopia.vault.nosql.access

import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.{Condition, Delete, OrderBy, Where}

/**
 * Common trait for access points that return parsed model data
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 * @tparam M Type of model returned
 * @tparam A The format in which model data is returned (E.g. a list of models)
 * @tparam V Format in which column values are returned (E.g. A single value or a vector of values)
 */
trait ModelAccess[+M, +A, +V] extends Access[A]
{
	// ABSTRACT	-------------------------
	
	/**
	 * @return The factory used for parsing accessed data
	 */
	def factory: FromResultFactory[M]
	
	/**
	 * Reads the value / values of an individual column
	 * @param column Column to read
	 * @param additionalCondition Additional search condition to apply (optional)
	 * @param order Ordering to use (optional)
	 * @param connection DB Connection (implicit)
	 * @return Value / values of that column (empty value(s) included)
	 */
	protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                         order: Option[OrderBy] = None)(implicit connection: Connection): V
	
	
	// IMPLEMENTED	---------------------
	
	final override def table = factory.table
	
	
	// COMPUTED	-------------------------
	
	/**
	 * @return The selection target used
	 */
	def target = factory.target
	
	
	// OTHER	-------------------------
	
	/**
	  * Checks whether there exist any results for a query with the specified condition
	  * @param condition A search condition (applied in addition to the global condition)
	  * @param connection DB Connection (implicit)
	  * @return Whether there exist any results for that search
	  */
	def exists(condition: Condition)(implicit connection: Connection) = factory.exists(mergeCondition(condition))
	
	/**
	  * Deletes all items accessible from this access points (only primary table is targeted)
	  * @param connection Database connection (implicit)
	  */
	def delete()(implicit connection: Connection): Unit =
		connection(Delete(target, table) + globalCondition.map { Where(_) })
	
	/**
	  * Deletes items which are accessible from this access point and fulfill the specified condition
	  * (only primary table is targeted)
	  * @param condition Deletion condition (applied in addition to the global condition)
	  * @param connection DB Connection (implicit)
	  */
	def deleteWhere(condition: Condition)(implicit connection: Connection): Unit =
		connection(Delete(target, table) + Where(mergeCondition(condition)))
	
	/**
	 * Reads the value of an individual column
	 * @param column Column to read
	 * @param condition Search condition to apply (will be added to the global condition)
	 * @param order Ordering to use (optional)
	 * @param connection DB Connection (implicit)
	 * @return Value of that column (may be empty)
	 */
	def findColumn(column: Column, condition: Condition, order: Option[OrderBy] = None)
	              (implicit connection: Connection) = readColumn(column, Some(condition), order)
	
	/**
	 * Reads the value of an individual attribute / column
	 * @param attributeName Name of the attribute to read
	 * @param condition Search condition to apply (will be added to the global condition)
	 * @param order Ordering to use (optional)
	 * @param connection DB Connection (implicit)
	 * @return Value of that attribute (may be empty)
	 */
	def findAttribute(attributeName: String, condition: Condition, order: Option[OrderBy])
	                 (implicit connection: Connection) =
		findColumn(table(attributeName), condition, order)
}
