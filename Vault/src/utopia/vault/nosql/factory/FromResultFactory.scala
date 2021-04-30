package utopia.vault.nosql.factory

import utopia.vault.sql.Extensions._
import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.{Result, Table}
import utopia.vault.sql.{Condition, Exists, JoinType, OrderBy, SelectAll, SqlTarget, Where}

/**
  * These factories are used for constructing object data from database results
  * @author Mikko Hilpinen
  * @since 8.7.2019, v1.1.1+
  */
trait FromResultFactory[+A]
{
	// ABSTRACT	---------------
	
	/**
	  * @return The primary table used for reading results data for this factory
	  */
	def table: Table
	
	/**
	  * @return The tables that are joined for complete results
	  */
	def joinedTables: Seq[Table]
	
	/**
	  * @return Joining style used
	  */
	def joinType: JoinType
	
	/**
	  * Parses a result into one or multiple (or zero) objects
	  * @param result A database query result to be parsed
	  * @return Parsed objects
	  */
	def apply(result: Result): Vector[A]
	
	
	// COMPUTED	---------------
	
	/**
	  * @return This factory's target that includes the primary table and possible joined tables
	  */
	def target = joinedTables.foldLeft(table: SqlTarget) { (r, t) => r.join(t, joinType) }
	/**
	  * @return The table(s) used by this factory (never empty)
	  */
	def tables = table +: joinedTables
	
	/**
	 * @return Whether this factory targets a single table only
	 */
	def targetsSingleTable = joinedTables.isEmpty
	
	
	// OTHER	---------------
	
	/**
	 * An iterator to the items accessible through this factory. This method uses limit and offset in queries
	 * so that tables with a large number of rows can be iterated while avoiding memory overload.
	 * @param condition Condition to apply to queries (optional)
	 * @param order Order to use in queries (optional)
	 * @param rowsPerQuery Number of rows to return on each query (default = use value defined in connection settings)
	 * @param connection DB Connection (implicit)
	 * @return An iterator to parsed items. The iterator must be used while the connection is still open.
	 */
	def iterator(condition: Option[Condition] = None, order: Option[OrderBy] = None,
	             rowsPerQuery: Int = Connection.settings.maximumAmountOfRowsCached)(implicit connection: Connection) =
		connection.iterator(SelectAll(target) + condition.map { Where(_) } + order, rowsPerQuery).flatMap(apply)
	
	/**
	  * Finds possibly multiple instances from the database
	  * @param where the condition with which the instances are filtered
	 *  @param order Ordering applied to the query (optional, None by default)
	  * @return Parsed instance data
	  */
	def getMany(where: Condition, order: Option[OrderBy] = None)(implicit connection: Connection) =
	{
		val baseStatement = SelectAll(target) + Where(where)
		val finalStatement = order.map { baseStatement + _ }.getOrElse(baseStatement)
		apply(connection(finalStatement))
	}
	
	/**
	  * Finds the instances with the specified ids
	  * @param ids Ids of the targeted instances
	  * @param order Ordering used (optional, None by default)
	  * @param connection DB Connection (implicit)
	  * @return All items with specified row ids
	  */
	def withIds(ids: Iterable[Value], order: Option[OrderBy] = None)(implicit connection: Connection) =
	{
		table.primaryColumn match
		{
			case Some(idColumn) => getMany(idColumn.in(ids), order)
			case None => Vector()
		}
	}
	
	/**
	  * Finds every single instance of this type from the database. This method should only be
	  * used in case of somewhat small tables.
	 *  @param order Order applied to the search (optional)
	  * @see #getMany(Condition)
	  */
	def getAll(order: Option[OrderBy] = None)(implicit connection: Connection) =
		apply(connection(SelectAll(target) + order))
	
	/**
	  * Checks whether an object exists for the specified query
	  * @param where A search condition
	  * @param connection Database connection (implicit)
	  * @return Whether there exists data in the DB for specified condition
	  */
	def exists(where: Condition)(implicit connection: Connection) = Exists(target, where)
	
	/**
	  * Checks whether there exists data for the specified index
	  * @param index An index in this factory's primary table
	  * @param connection Database connection (implicit)
	  * @return Whether there exists data for the specified index
	  */
	def exists(index: Value)(implicit connection: Connection) = Exists.index(table, index)
	
	/**
	 * Retrieves an object's data from the database and parses it to a proper instance
	 * @param index the index / primary key with which the data is read
	 * @return database data parsed into an instance. None if there was no data available.
	 */
	def get(index: Value)(implicit connection: Connection): Option[A] =
		table.primaryColumn.flatMap { column => get(column <=> index) }
	
	/**
	 * Retrieves an object's data from the database and parses it to a proper instance
	 * @param where The condition with which the row is found from the database (will be limited to
	 * the first result row)
	 * @param order Ordering applied (optional, None by default)
	 * @return database data parsed into an instance. None if no data was found with the provided
	 * condition
	 */
	def get(where: Condition, order: Option[OrderBy] = None)(implicit connection: Connection) =
		getMany(where, order).headOption
}
