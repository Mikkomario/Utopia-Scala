package utopia.vault.nosql.access

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.{Condition, Delete, Where}

/**
 * Common trait for access points that return parsed model data
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 * @tparam M Type of model returned
 * @tparam A The format in which model data is returned (Eg. a list of models)
 */
trait ModelAccess[+M, +A] extends Access[A]
{
	// ABSTRACT	-------------------------
	
	/**
	 * @return The factory used for parsing accessed data
	 */
	def factory: FromResultFactory[M]
	
	
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
}
