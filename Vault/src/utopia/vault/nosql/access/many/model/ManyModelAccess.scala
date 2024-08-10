package utopia.vault.nosql.access.many.model

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Result}
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.many.ManyAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, JoinType, OrderBy, Select, SqlSegment, SqlTarget, Where}

/**
 * Used for accessing multiple models at a time from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyModelAccess[+A] extends ManyAccess[A] with DistinctModelAccess[A, Seq[A], Seq[Value]]
{
	// COMPUTED --------------------------------
	
	/**
	 * @param connection DB Connection (implicit)
	 * @return An iterator to all models accessible through this access point. The iterator is valid
	 *         only while the connection is kept open.
	 */
	def iterator(implicit connection: Connection) = factory.iterator(accessCondition)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def defaultOrdering = factory.defaultOrdering
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy], joins: Seq[Joinable],
	                            joinType: JoinType)(implicit connection: Connection) =
	{
		lazy val appliedOrdering = order.orElse(defaultOrdering)
		condition.filterNot { _.isAlwaysTrue } match {
			case Some(condition) =>
				// If the condition is impossible to fulfill, skips this query altogether
				if (condition.isAlwaysFalse)
					Empty
				else
					factory.findMany(condition, appliedOrdering, joins, joinType)
			
			case None => factory.getAll(appliedOrdering)
		}
	}
	
	override protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                                  order: Option[OrderBy] = None, joins: Seq[Joinable] = Empty,
	                                  joinType: JoinType = Inner)(implicit connection: Connection) =
		_read(additionalCondition, order, joins, joinType) { Select(_, column) }.rowValues
	
	
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
	  * Reads values of an individual column. Only yields distinct values.
	  * @param column Read column
	  * @param joins Applied joins
	  * @param connection Implicit DB connection
	  * @return Distinct accessible values of the targeted column
	  */
	def pullDistinct(column: Column, joins: Joinable*)(implicit connection: Connection) =
		readDistinct(column, None, joins)
	/**
	  * Searches for distinct values of an individual column.
	  * @param column Read column
	  * @param condition Search condition to apply
	  * @param joins Applied joins
	  * @param connection Implicit DB connection
	  * @return Distinct accessible values of the targeted column
	  */
	def findDistinct(column: Column, condition: Condition, joins: Joinable*)(implicit connection: Connection) =
		readDistinct(column, Some(condition), joins)
	
	/**
	  * Pulls a column-to-column map based on the accessible items
	  * @param keyColumn Column used as map keys (should contain unique values)
	  * @param valueColumn Column used as map values
	  * @param joins Joins to apply (optional)
	  * @param con Implicit DB Connection
	  * @return A map that contains all read key-value pairs (as values)
	  */
	def pullColumnMap(keyColumn: Column, valueColumn: Column, joins: Joinable*)(implicit con: Connection) =
		readColumnMap(keyColumn, valueColumn, None, joins)
	/**
	  * Pulls a column-to-columns map based on the accessible items
	  * @param keyColumn Column used as map keys
	  * @param valueColumn Column used as individual values
	  * @param joins Joins to apply (optional)
	  * @param con Implicit DB Connection
	  * @return A map that contains all read values grouped by keys (as values)
	  */
	def pullColumnMultiMap(keyColumn: Column, valueColumn: Column, joins: Joinable*)(implicit con: Connection) =
		readColumnMultiMap(keyColumn, valueColumn, None, joins)
	
	/**
	  * Finds a column-to-column map based on a subset of the accessible items
	  * @param keyColumn Column used as map keys (should contain unique values)
	  * @param valueColumn Column used as map values
	  * @param condition Search / filter condition to apply
	  * @param joins Joins to apply (optional)
	  * @param con Implicit DB Connection
	  * @return A map that contains all read key-value pairs (as values)
	  * @see [[pullColumnMap]]
	  */
	def findColumnMap(keyColumn: Column, valueColumn: Column, condition: Condition, joins: Joinable*)
	                 (implicit con: Connection) =
		readColumnMap(keyColumn, valueColumn, Some(condition), joins)
	/**
	  * Finds a column-to-columns map based on a subset of the accessible items
	  * @param keyColumn Column used as map keys
	  * @param valueColumn Column used as individual values
	  * @param condition A search condition to apply
	  * @param joins Joins to apply (optional)
	  * @param con Implicit DB Connection
	  * @return A map that contains all read values grouped by keys (as values)
	  */
	def findColumnMultiMap(keyColumn: Column, valueColumn: Column, condition: Condition, joins: Joinable*)
	                      (implicit con: Connection) =
		readColumnMultiMap(keyColumn, valueColumn, Some(condition), joins)
	
	private def readDistinct(column: Column, condition: Option[Condition], joins: Iterable[Joinable])
	                        (implicit connection: Connection) =
		_read(condition, joins = joins) { Select.distinct(_, column) }.rowValues
		
	private def readColumnMap(keyColumn: Column, valueColumn: Column, condition: Option[Condition],
	                          joins: Iterable[Joinable])
	                 (implicit con: Connection) =
	{
		_read(condition, joins = joins) { Select(_, Pair(keyColumn, valueColumn)) }
			.rows.view.map { row => row(keyColumn) -> row(valueColumn) }.toMap
	}
	private def readColumnMultiMap(keyColumn: Column, valueColumn: Column, condition: Option[Condition],
	                               joins: Iterable[Joinable])
	                              (implicit con: Connection) =
	{
		_read(condition, joins = joins) { Select(_, Pair(keyColumn, valueColumn)) }
			.rows.groupMap { _(keyColumn) } { _(valueColumn) }
	}
	
	private def _read(additionalCondition: Option[Condition] = None, order: Option[OrderBy] = None,
	                  joins: Iterable[Joinable] = Empty, joinType: JoinType = Inner)
	                 (selectStatement: SqlTarget => SqlSegment)
	                 (implicit connection: Connection) =
	{
		val condition = mergeCondition(additionalCondition).filterNot { _.isAlwaysTrue }
		if (condition.exists { _.isAlwaysFalse })
			Result.empty
		else
			// Applies the joins, additional condition & ordering
			connection(selectStatement(joins.foldLeft(target) { _.join(_, joinType) }) +
				condition.map { Where(_) } + order.orElse(defaultOrdering))
	}
}