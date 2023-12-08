package utopia.vault.nosql.access.many.model

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.many.ManyAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, JoinType, OrderBy, Select, Where}

/**
 * Used for accessing multiple models at a time from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyModelAccess[+A] extends ManyAccess[A] with DistinctModelAccess[A, Vector[A], Vector[Value]]
{
	// COMPUTED --------------------------------
	
	/**
	 * @param connection DB Connection (implicit)
	 * @return An iterator to all models accessible through this access point. The iterator is valid
	 *         only while the connection is kept open.
	 */
	def iterator(implicit connection: Connection) = factory.iterator(accessCondition)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy], joins: Seq[Joinable],
	                            joinType: JoinType)(implicit connection: Connection) =
	{
		condition match {
			case Some(condition) => factory.findMany(condition, order, joins, joinType)
			case None => factory.getAll(order)
		}
	}
	
	override protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                                  order: Option[OrderBy] = None, joins: Seq[Joinable] = Vector(),
	                                  joinType: JoinType = Inner)(implicit connection: Connection) =
	{
		// Forms the query first
		val statement = Select(joins.foldLeft(target) { _.join(_, joinType) }, column) +
			mergeCondition(additionalCondition).map { Where(_) } + order.orElse(factory.defaultOrdering)
		// Applies the query and parses results
		connection(statement).rowValues
	}
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param f A mapping function
	  * @param c Implicit DB Connection
	  * @tparam B Type of map results
	  * @return A map where each accessible item is mapped to a map result
	  */
	def toMapBy[B](f: A => B)(implicit c: Connection) = pull.iterator.map { a => f(a) -> a }.toMap
	
	/**
	 * @param order Order to use in the results
	 * @param connection DB Connection (implicit)
	 * @return An iterator to all models accessible from this access point. The iterator is usable
	 *         only while the connection is kept open.
	 */
	def orderedIterator(order: OrderBy)(implicit connection: Connection) =
		factory.iterator(accessCondition, Some(order))
	
	/**
	  * Pulls a column-to-column map based on the accessible items
	  * @param keyColumn Column used as map keys (should contain unique values)
	  * @param valueColumn Column used as map values
	  * @param joins Joins to apply (optional)
	  * @param con Implicit DB Connection
	  * @return A map that contains all read key-value pairs (as values)
	  */
	def pullColumnMap(keyColumn: Column, valueColumn: Column, joins: Joinable*)(implicit con: Connection) = {
		val statement = Select(joins.foldLeft(target) { _ join _ }, Vector(keyColumn, valueColumn)) +
			accessCondition.map { Where(_) }
		con(statement).rows.map { row => row(keyColumn) -> row(valueColumn) }.toMap
	}
	/**
	  * Pulls a column-to-columns map based on the accessible items
	  * @param keyColumn Column used as map keys
	  * @param valueColumn Column used as individual values
	  * @param joins Joins to apply (optional)
	  * @param con Implicit DB Connection
	  * @return A map that contains all read values grouped by keys (as values)
	  */
	def pullColumnMultiMap(keyColumn: Column, valueColumn: Column, joins: Joinable*)(implicit con: Connection) = {
		val statement = Select(joins.foldLeft(target) { _ join _ }, Vector(keyColumn, valueColumn)) +
			accessCondition.map { Where(_) }
		con(statement).rows.map { row => row(keyColumn) -> row(valueColumn) }.asMultiMap
	}
}