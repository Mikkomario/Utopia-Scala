package utopia.vault.nosql.factory

import utopia.flow.util.CollectionExtensions._
import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Column, Result, Row}
import utopia.vault.sql.{Condition, Limit, OrderBy, SelectAll, Where}
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
	
	
	// IMPLEMENTED	----------------
	
	override def apply(result: Result): Vector[A] = result.rows.flatMap(parseIfPresent)
	
	/**
	  * Retrieves an object's data from the database and parses it to a proper instance
	  * @param index the index / primary key with which the data is read
	  * @return database data parsed into an instance. None if there was no data available.
	  */
	override def get(index: Value)(implicit connection: Connection): Option[A] =
	{
		table.primaryColumn.flatMap { column => get(column <=> index) }
	}
	
	/**
	  * Retrieves an object's data from the database and parses it to a proper instance
	  * @param where The condition with which the row is found from the database (will be limited to
	  * the first result row)
	  * @return database data parsed into an instance. None if no data was found with the provided
	  * condition
	  */
	override def get(where: Condition, order: Option[OrderBy] = None)(implicit connection: Connection) =
	{
		val baseStatement = SelectAll(target) + Where(where)
		val finalStatement = order.map { baseStatement + _ }.getOrElse(baseStatement) + Limit(1)
		connection(finalStatement).rows.headOption.flatMap(parseIfPresent)
	}
	
	
	// OTHER	--------------------
	
	/**
	 * Parses data from a row, provided that the row contains data for the primary table
	 * @param row Parsed row
	 * @return parsed data. None if row didn't contain data for the primary table
	 */
	def parseIfPresent(row: Row): Option[A] =
	{
		if (row.containsDataForTable(table))
		{
			apply(row) match
			{
				case Success(parsed) => Some(parsed)
				// Lets ErrorHandling handle the possible errors
				case Failure(error) => ErrorHandling.modelParsePrinciple.handle(error); None
			}
		}
		else
			None
	}
	
	/**
	  * Takes a number of items with this factory
	  * @param maxNumberOfItems Maximum number of items returned
	  * @param order Ordering used
	  * @param condition A filter condition (None if not filtered, default)
	  * @param connection DB Connection (implicit)
	  * @return
	  */
	def take(maxNumberOfItems: Int, order: OrderBy, condition: Option[Condition] = None)(implicit connection: Connection) =
	{
		connection(SelectAll(target) + condition.map { Where(_) } + order +
			Limit(maxNumberOfItems)).rows.flatMap(parseIfPresent)
	}
	
	/**
	  * Finds the top / max row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param where Additional search condition (optional)
	  * @param connection Database connection
	  */
	def getMax(orderColumn: Column, where: Option[Condition])(implicit  connection: Connection) =
		getWithOrder(OrderBy.descending(orderColumn), where)
	
	/**
	  * Finds the top / max row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param connection Database connection
	  */
	def getMax(orderColumn: Column)(implicit  connection: Connection): Option[A] = getMax(orderColumn, None)
	
	/**
	  * Finds the top / max row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param where Additional search condition (optional)
	  * @param connection Database connection
	  */
	def getMax(orderColumn: Column, where: Condition)(implicit  connection: Connection): Option[A] = getMax(orderColumn, Some(where))
	
	/**
	  * Finds top / max row / model based on provided ordering property. Uses primary table's columns by default but may
	  * use columns from other tables if such property couldn't be found from the primary table.
	  * @param orderProperty The name of the ordering property
	  * @param connection Database connection
	  */
	def getMax(orderProperty: String)(implicit connection: Connection): Option[A] = findColumn(orderProperty).flatMap(getMax)
	
	/**
	  * Finds top / max row / model based on provided ordering property. Uses primary table's columns by default but may
	  * use columns from other tables if such property couldn't be found from the primary table.
	  * @param orderProperty The name of the ordering property
	  * @param where Additional search condition (optional)
	  * @param connection Database connection
	  */
	def getMax(orderProperty: String, where: Condition)(implicit connection: Connection): Option[A] =
		findColumn(orderProperty).flatMap { getMax(_, where) }
	
	/**
	  * Finds the bottom / min row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param connection Database connection
	  */
	def getMin(orderColumn: Column, where: Option[Condition])(implicit connection: Connection) =
		getWithOrder(OrderBy.ascending(orderColumn), where)
	
	/**
	  * Finds the bottom / min row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param connection Database connection
	  */
	def getMin(orderColumn: Column)(implicit connection: Connection): Option[A] = getMin(orderColumn, None)
	
	/**
	  * Finds the bottom / min row / model based on provided ordering column
	  * @param orderColumn A column based on which ordering is made
	  * @param connection Database connection
	  */
	def getMin(orderColumn: Column, where: Condition)(implicit connection: Connection): Option[A] = getMin(orderColumn, Some(where))
	
	/**
	  * Finds bottom / min row / model based on provided ordering property. Uses primary table's columns by default but may
	  * use columns from other tables if such property couldn't be found from the primary table.
	  * @param orderProperty The name of the ordering property
	  * @param connection Database connection
	  */
	def getMin(orderProperty: String)(implicit connection: Connection): Option[A] = findColumn(orderProperty).flatMap(getMin)
	
	/**
	  * Finds bottom / min row / model based on provided ordering property. Uses primary table's columns by default but may
	  * use columns from other tables if such property couldn't be found from the primary table.
	  * @param orderProperty The name of the ordering property
	  * @param connection Database connection
	  */
	def getMin(orderProperty: String, where: Condition)(implicit connection: Connection): Option[A] =
		findColumn(orderProperty).flatMap { getMin(_, where) }
	
	/**
	 * Finds an item from the target without any ordering or conditions
	 * @param connection Database connection (implicit)
	 * @return The first item found
	 */
	def getAny()(implicit connection: Connection) = connection(SelectAll(target) + Limit(1))
		.rows.headOption.flatMap(parseIfPresent)
	
	/**
	  * Performs an operation on each of the targeted entities
	  * @param condition A condition for finding target entities (optional)
	  * @param operation An operation performed for each entity
	  * @param connection DB Connection
	  * @tparam U Arbitrary result type
	  */
	def foreachWhere[U](condition: Option[Condition])(operation: A => U)(implicit connection: Connection) =
	{
		val select = SelectAll(target)
		val statement = condition match
		{
			case Some(condition) => select + Where(condition)
			case None => select
		}
		connection.foreach(statement) { parseIfPresent(_).foreach(operation) }
	}
	
	/**
	 * Performs an operation on each of the targeted entities
	 * @param where A condition for finding target entities
	 * @param operation An operation performed for each entity
	 * @param connection DB Connection
	  * @tparam U Arbitrary result type
	 */
	def foreachWhere[U](where: Condition)(operation: A => U)(implicit connection: Connection): Unit =
		foreachWhere(Some(where))(operation)
	
	/**
	  * Performs an operation on all entities accessible from this factory
	  * @param operation An operation performed for each entity
	  * @param connection DB Connection
	  * @tparam U Arbitrary result type
	  */
	def foreach[U](operation: A => U)(implicit connection: Connection) = foreachWhere(None)(operation)
	
	/**
	 * Folds entities into a single value
	 * @param where A condition for finding targeted entities
	 * @param start Starting value
	 * @param f A function that adds one entity to the final value
	 * @param connection DB Connection
	 * @tparam B Type of result
	 * @return result once all entities have been folded
	 */
	def fold[B](where: Condition)(start: B)(f: (B, A) => B)(implicit connection: Connection) =
		connection.fold(SelectAll(target) + Where(where))(start) { (v, row) => parseIfPresent(row).map { f(v, _) }.getOrElse(v) }
	
	/**
	 * Maps entities, then reduces mapped values
	 * @param where A condition for finding targeted entities
	 * @param map A mapping function
	 * @param reduce A function that reduces (combines) mapped results
	 * @param connection DB Connection
	 * @tparam B Type of map result
	 * @return Reduce result. None if no entities where found
	 */
	def mapReduce[B](where: Condition)(map: A => B)(reduce: (B, B) => B)(implicit connection: Connection) =
		connection.flatMapReduce(SelectAll(target) + Where(where)) { row => parseIfPresent(row).map(map) }(reduce)
	
	private def getWithOrder(orderBy: OrderBy, where: Option[Condition] = None)(implicit connection: Connection) =
	{
		where match
		{
			case Some(condition) => get(condition, Some(orderBy))
			case None => connection(SelectAll(target) + orderBy + Limit(1)).rows.headOption.flatMap(parseIfPresent)
		}
	}
	
	private def findColumn(propertyName: String) = table.find(propertyName).orElse {
		joinedTables.findMap { _.find(propertyName) } }
}
