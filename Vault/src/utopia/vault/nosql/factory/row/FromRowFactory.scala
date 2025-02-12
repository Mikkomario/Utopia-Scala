package utopia.vault.nosql.factory.row

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.error.ColumnNotFoundException
import utopia.vault.model.immutable.{Column, Result, Row}
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql._
import utopia.vault.util.ErrorHandling

import scala.util.{Failure, Success, Try}

/**
  * These factories are used for converting database row data into objects. These factories are able to parse an object
  * from a single row (which may contain joined data)
  * @author Mikko Hilpinen
  * @since 10.7.2019, v1.2+
  */
trait FromRowFactory[+A] extends FromResultFactory[A]
{
	// ABSTRACT	--------------------
	
	/**
	  * Converts a row into an object
	  * @param row Row to be converted
	  * @return Parsed object. Failure if no object could be parsed.
	  */
	def apply(row: Row): Try[A]
	
	
	// COMPUTED --------------------
	
	/**
	  * Finds an item from the target without any additional ordering or conditions
	  * @param connection Database connection (implicit)
	  * @return The first item found
	  */
	def any(implicit connection: Connection) =
		connection(select + defaultOrdering + Limit(1)).rows.headOption.flatMap(parseIfPresent)
	
	
	// IMPLEMENTED	----------------
	
	override def apply(result: Result): Seq[A] = fromUniqueRows(result.rows)(parseIfPresent)
	
	override def find(where: Condition, order: Option[OrderBy] = None, joins: Seq[Joinable] = Empty,
	                  joinType: JoinType = Inner)(implicit connection: Connection) =
		connection(expandedSelect(joins, joinType) + Where(where) + order.orElse(defaultOrdering) + Limit(1))
			.rows.headOption.flatMap(parseIfPresent)
	
	
	// OTHER	--------------------
	
	/**
	  * @param propertyName Property name to search
	  * @return A column used by this factory using that property name
	  */
	def column(propertyName: String) = table.find(propertyName)
		.orElse { joinedTables.findMap { _.find(propertyName) } }
		.getOrElse { throw new ColumnNotFoundException(s"$target doesn't contain column for property $propertyName") }
	
	/**
	  * Parses data from a row, provided that the row contains data for the primary table
	  * @param row Parsed row
	  * @return parsed data. None if row didn't contain data for the primary table, or if parsing failed.
	  */
	def parseIfPresent(row: Row): Option[A] = if (row.containsDataForTable(table)) tryParse(row) else None
	/**
	  * Attempts to parse an item from the specified row.
	  * Handles parse failures using [[ErrorHandling.modelParsePrinciple]]
	  * @param row A row to parse the item from
	  * @return Parsed item. None if no item could be parsed.
	  */
	def tryParse(row: Row): Option[A] = apply(row) match {
		case Success(parsed) => Some(parsed)
		// Lets ErrorHandling handle the possible errors
		case Failure(error) => ErrorHandling.modelParsePrinciple.handle(error); None
	}
	
	/**
	  * Reads accessible data from the DB. Includes additional information, besides the parsed row entries.
	  * @param extraTarget An additional target to include in the select
	  * @param joins Joins to apply
	  * @param joinType Type of joins to apply (default = inner)
	  * @param condition Search condition to apply (default = None)
	  * @param order Ordering to apply (default = None = use default ordering)
	  * @param parse A function for parsing the additional data from row information
	  * @param connection Implicit DB connection
	  * @tparam B Type of parsed additional data
	  * @return Accessible data, including additional parsed data
	  */
	def getIncluding[B](extraTarget: SelectTarget, joins: Seq[Joinable], joinType: JoinType = Inner,
	                    condition: Option[Condition] = None, order: Option[OrderBy] = None)
	                   (parse: Row => B)(implicit connection: Connection) =
	{
		val rows = connection(expandedSelect(joins, joinType, Some(extraTarget)) + condition.map(Where.apply) +
			order.orElse(defaultOrdering))
			.rows
		fromUniqueRows(rows) { row => parseIfPresent(row).map { _ -> parse(row) } }
	}
	/**
	  * Reads accessible data from the DB. Includes an additional column outside the default selection.
	  * @param column Column to include in the results
	  * @param joins Joins to apply
	  * @param joinType Type of joins to apply (default = inner)
	  * @param condition Search condition to apply (default = None)
	  * @param order Ordering to apply (default = None = use default ordering)
	  * @param parse A function for parsing the column value
	  * @param connection Implicit DB connection
	  * @tparam B Type of parsed additional data
	  * @return Accessible data, including additional parsed data
	  */
	def getWithColumn[B](column: Column, joins: Seq[Joinable], joinType: JoinType = Inner,
	                     condition: Option[Condition] = None, order: Option[OrderBy] = None)
	                    (parse: Value => B)(implicit connection: Connection) =
		getIncluding(column, joins, joinType, condition, order) { row => parse(row(column)) }
	
	/**
	  * Takes a number of items with this factory
	  * @param maxNumberOfItems Maximum number of items returned
	  * @param order            Ordering used
	  * @param condition        A filter condition (None if not filtered, default)
	  * @param connection       DB Connection (implicit)
	  * @return
	  */
	def take(maxNumberOfItems: Int, order: OrderBy, condition: Option[Condition] = None)
	        (implicit connection: Connection) =
		connection(select + condition.map { Where(_) } + order + Limit(maxNumberOfItems)).rows.flatMap(parseIfPresent)
	
	/**
	  * Finds the top / max row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param where       Additional search condition (optional)
	  * @param connection  Database connection
	  */
	def getMax(orderColumn: Column, where: Option[Condition])(implicit connection: Connection) =
		getWithOrder(OrderBy.descending(orderColumn), where)
	
	/**
	  * @param ordering Ordering to apply
	  * @param connection Implicit database connection
	  * @return First available result using that ordering
	  */
	def firstUsing(ordering: OrderBy)(implicit connection: Connection) =
		connection(select + ordering + Limit(1)).rows.headOption.flatMap(parseIfPresent)
	/**
	  * @param orderColumn Column to base ordering on
	  * @param connection Implicit DB Connection
	  * @return The maximum / top item when comparing that column
	  */
	def maxBy(orderColumn: Column)(implicit connection: Connection) =
		firstUsing(OrderBy.descending(orderColumn))
	
	/**
	  * Finds the largest available instance that fulfils the specified condition
	  * @param orderColumn Column to use for ordering
	  * @param where A search condition
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type used (default = inner)
	  * @param connection Implicit DB Connection
	  * @return Maximum item (according to ordering) found
	  */
	def findMaxBy(orderColumn: Column, where: Condition, joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)
	           (implicit connection: Connection) =
		find(where, Some(OrderBy.descending(orderColumn)), joins, joinType)
	/**
	  * Finds the largest available instance that fulfils the specified condition
	  * @param orderProperty Name of the property to use for ordering
	  * @param where A search condition
	  * @param connection Implicit DB Connection
	  * @return Maximum item (according to ordering) found
	  */
	def findMaxBy(orderProperty: String, where: Condition)(implicit connection: Connection): Option[A] =
		findMaxBy(column(orderProperty), where)
	
	/**
	  * @param orderingProperty Name of the property on which the ordering should be based on
	  * @param connection Implicit DB Connection
	  * @return The maximum / top accessible item when comparing that property
	  */
	def maxBy(orderingProperty: String)(implicit connection: Connection): Option[A] = maxBy(column(orderingProperty))
	
	/**
	  * Finds the bottom / min row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param connection  Database connection
	  */
	def getMin(orderColumn: Column, where: Option[Condition])(implicit connection: Connection) =
		getWithOrder(OrderBy.ascending(orderColumn), where)
	
	/**
	  * @param orderColumn Column to use for ordering
	  * @param connection Implicit DB Connection
	  * @return The minimum / bottom item when comparing that column
	  */
	def minBy(orderColumn: Column)(implicit connection: Connection) =
		firstUsing(OrderBy.ascending(orderColumn))
	
	/**
	  * Finds the bottom / min row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param where Search condition to use
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = inner)
	  * @param connection  Database connection
	  */
	def findMinBy(orderColumn: Column, where: Condition, joins: Seq[Joinable] = Empty,
	              joinType: JoinType = Inner)(implicit connection: Connection) =
		find(where, Some(OrderBy.descending(orderColumn)), joins, joinType)
	
	/**
	  * @param orderingProperty Property used when comparing instances
	  * @param connection Implicit DB Connection
	  * @return The smallest available item when comparing that property
	  */
	def minBy(orderingProperty: String)(implicit connection: Connection): Option[A] = minBy(column(orderingProperty))
	
	/**
	  * @param orderingProperty Name of the ordering property to use
	  * @param where Search condition to apply
	  * @param connection Implicit DB Connection
	  * @return The smallest found result, based on the specified property name
	  */
	def findMinBy(orderingProperty: String, where: Condition)(implicit connection: Connection): Option[A] =
		findMinBy(column(orderingProperty), where)
	
	/**
	  * Finds an item using a join (for searching)
	  * @param joined Target to join
	  * @param where Search condition to apply
	  * @param order Ordering to use (default = None = use default ordering)
	  * @param joinType Join type to apply
	  * @param connection Implicit DB Connection
	  * @return The first found item
	  */
	def findLinked(joined: Joinable, where: Condition, order: Option[OrderBy] = None, joinType: JoinType = Inner)
	              (implicit connection: Connection) =
		find(where, order, Single(joined), joinType)
	
	/**
	  * Performs an operation on each of the targeted entities
	  * @param condition  A condition for finding target entities (optional)
	  * @param operation  An operation performed for each entity
	  * @param connection DB Connection
	  * @tparam U Arbitrary result type
	  */
	def foreachWhere[U](condition: Option[Condition])(operation: A => U)(implicit connection: Connection) = {
		val statement = condition match {
			case Some(condition) => select + Where(condition)
			case None => select
		}
		connection.foreach(statement) { parseIfPresent(_).foreach(operation) }
	}
	
	/**
	  * Performs an operation on each of the targeted entities
	  * @param where      A condition for finding target entities
	  * @param operation  An operation performed for each entity
	  * @param connection DB Connection
	  * @tparam U Arbitrary result type
	  */
	def foreachWhere[U](where: Condition)(operation: A => U)(implicit connection: Connection): Unit =
		foreachWhere(Some(where))(operation)
	
	/**
	  * Performs an operation on all entities accessible from this factory
	  * @param operation  An operation performed for each entity
	  * @param connection DB Connection
	  * @tparam U Arbitrary result type
	  */
	def foreach[U](operation: A => U)(implicit connection: Connection) = foreachWhere(None)(operation)
	
	/**
	  * Folds entities into a single value
	  * @param where      A condition for finding targeted entities
	  * @param start      Starting value
	  * @param f          A function that adds one entity to the final value
	  * @param connection DB Connection
	  * @tparam B Type of result
	  * @return result once all entities have been folded
	  */
	def fold[B](where: Condition)(start: B)(f: (B, A) => B)(implicit connection: Connection) =
		connection.fold(select + Where(where))(start) { (v, row) =>
			parseIfPresent(row).map { f(v, _) }.getOrElse(v)
		}
	
	/**
	  * Maps entities, then reduces mapped values
	  * @param where      A condition for finding targeted entities
	  * @param map        A mapping function
	  * @param reduce     A function that reduces (combines) mapped results
	  * @param connection DB Connection
	  * @tparam B Type of map result
	  * @return Reduce result. None if no entities where found
	  */
	def mapReduce[B](where: Condition)(map: A => B)(reduce: (B, B) => B)(implicit connection: Connection) =
		connection.flatMapReduce(select + Where(where)) { row => parseIfPresent(row).map(map) }(reduce)
	
	private def getWithOrder(orderBy: OrderBy, where: Option[Condition] = None)
	                        (implicit connection: Connection) =
		where match {
			case Some(condition) => find(condition, Some(orderBy))
			case None => connection(select + orderBy + Limit(1)).rows.headOption.flatMap(parseIfPresent)
		}
	
	// Makes sure duplicate rows are not present by only including each index (group) once
	// (Only works when all included tables use indexing)
	private def fromUniqueRows[B](rows: Seq[Row])(parse: Row => Option[B]) = {
		val indices = tables.flatMap { table => table.primaryColumn }
		if (indices.hasSize.of(tables) && rows.view.take(4).exists { row => indices.forall { row(_).isDefined } })
			rows.view.distinctBy { row => indices.map(row.apply) }.flatMap(parse).toOptimizedSeq
		else
			rows.flatMap(parse)
	}
}
