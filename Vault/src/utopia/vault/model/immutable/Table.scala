package utopia.vault.model.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single, ViewGraphNode}
import utopia.flow.generic.model.immutable.{ModelDeclaration, Value}
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.vault.database.{Connection, References, TableUpdateListener, Triggers}
import utopia.vault.model.error.{ColumnNotFoundException, NoReferenceFoundException}
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.factory.row.model.FromRowModelFactory
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql._

import scala.util.{Failure, Success}

object Table
{
	/**
	  * @param name Name of this table, as it appears in the database
	  * @param databaseName Name of the database in which this table appears
	  * @param columns Columns contained within this table
	  * @return A new table
	  */
	def apply(name: String, databaseName: String, columns: Seq[Column]) =
		new Table(name, databaseName, columns)
}

/**
  * Represents a database table, consisting of one or more columns.
  * @author Mikko Hilpinen
  * @since 9.3.2017
  */
case class Table private(name: String, databaseName: String, _columns: Seq[Column]) extends SqlTarget with Joinable
{
	// ATTRIBUTES    ---------------------------
	
	/**
	  * @return The name of this table in sql format (same as original name but surrounded with `backticks`).
	  */
	lazy val sqlName = s"`$name`"
	
	/**
	  * Columns in this table. Each includes a reference to this table, also.
	  */
	lazy val columns = _columns.map { TableColumn(this, _) }
	/**
	  * The primary column for this table. Not all tables do have primary columns though.
	  */
	lazy val primaryColumn = columns.find { _.isPrimary }
	/**
	  * A map that contains all the columns of this table with their lower case property names as keys
	  */
	private lazy val columnsByPropName = columns.view.map { c => c.name.toLowerCase -> c }.toMap
	private lazy val columnsByColName = columns.view.map { c => c.columnName -> c }.toMap
	
	/**
	  * A model declaration based on this table
	  */
	lazy val toModelDeclaration = ModelDeclaration(_columns)
	
	override lazy val toSqlSegment = SqlSegment(sqlName, Empty, Some(databaseName), Set(this))
	
	
	// COMPUTED PROPERTIES    ------------------
	
	override def toString = name
	
	/**
	  * @return Whether this table has a primary column
	  */
	def hasPrimaryColumn = primaryColumn.isDefined
	/**
	  * Whether the table has an index (primary key) that uses auto-increment
	  */
	def usesAutoIncrement = primaryColumn.exists { _.usesAutoIncrement }
	
	/**
	  * @return A factory for storable instances from this table
	  */
	def toFactory = FromRowModelFactory(this)
	
	/**
	 * @throws ColumnNotFoundException If this table does not specify a primary key column
	 * @return The primary key / index used by this table
	 */
	@throws[ColumnNotFoundException]("If this table does not specify a primary key column")
	def getPrimaryKey =
		primaryColumn.getOrElse { throw new ColumnNotFoundException(s"$this does not specify a primary index") }
	
	/**
	  * @param connection Database connection
	  * @return All defined indices for this table in database
	  */
	@deprecated("Deprecated for removal at some point. It is recommended to use the more advanced database access interfaces", "v1.21.1")
	def allIndices(implicit connection: Connection) =
		Select.index(this).execute().indicesForTable(this)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def tables = Single(this)
	override def joinTypes = Empty
	
	override def contains(table: Table): Boolean = table == this
	
	override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType = Inner) = {
		// If already part of the origin tables, no join is created
		if (originTables.contains(this))
			Success(Empty)
		else
			// Finds the first table referencing (or being referenced by) the provided table and uses
			// that for a join
			originTables.findMap { left => References.connectionBetween(left, this) } match {
				case Some(Pair(leftColumn, rightColumn)) =>
					Success(Single(Join(leftColumn, TableColumn(this, rightColumn), joinType)))
				case None =>
					// Secondarily, finds indirect references
					References.toBiDirectionalLinkGraphFrom(this)
						.cheapestRoutesTo[Int] {
							node: ViewGraphNode[Table, (Reference, Boolean)] => originTables.contains(node.value) } {
							edge =>
								// Evaluates the routes by:
								//      1. Length
								//      2. Whether joining via nullable columns
								//      3. Whether joining the same direction as the reference (avoiding many-to-one links)
								//      4. Route ambiguity (based on the number of tables that may be involved)
								val (reference, sameDirection) = edge.value
								val referenceTypeCost = if (reference.from.allowsNull) 1000 else 0
								// Note: We're moving in the graph from the join target to a join origin,
								//       so reference "same direction" has a different meaning
								val directionCost = if (sameDirection) 100 else 0
								val nextTable = (if (sameDirection) reference.to else reference.from).table
								val referenceCountCost = References.from(nextTable).size + References.to(nextTable).size
								
								10000 + referenceTypeCost + directionCost + referenceCountCost
						}
						.cheapest match
					{
						case Some(result) =>
							Success(result.anyRoute.view.reverse
								.map { edge =>
									val (reference, isReversed) = edge.value
									if (isReversed) reference.reverse.toJoin else reference.toJoin
								}
								.toOptimizedSeq)
						case None =>
							Failure(new NoReferenceFoundException(
								s"Cannot find a reference between ${
									originTables.map { _.name }.mkString(" + ") } and $name. Only found references: [${
									(tables :+ this).iterator.flatMap(References.from).mkString(", ")}]"))
					}
			}
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param propertyName Name of the database property matching the targeted column
	  * @throws NoSuchElementException If this table doesn't contain the specified column
	  * @return Targeted column
	  * @see [[find]]
	  */
	@throws[NoSuchElementException]("If this table doesn't contain the specified column")
	def apply(propertyName: String) = find(propertyName).getOrElse {
		throw new NoSuchElementException(
			s"Table '$name' doesn't contain property '$propertyName'. Available properties: [${
				columns.view.map { _.name }.mkString(", ")}]")
	}
	/**
	  * Finds the columns matching the provided property names
	  */
	def apply(propertyNames: Iterable[String]) =
		columns.filter { column => propertyNames.exists { _ == column.name } }
	/**
	  * Finds columns matching the provided property names
	  */
	def apply(propertyName: String, second: String, others: String*): Seq[TableColumn] =
		apply(Pair(propertyName, second) ++ others)
	
	/**
	  * Finds a column with the provided database model property name. Returns None if no such column exists in
	  * this table
	  */
	def find(propertyName: String) = columnsByPropName.get(propertyName.toLowerCase)
	/**
	  * Finds a column with the specified column name. Returns None if no such column exists.
	  */
	def findColumnWithName(columnName: String) = columnsByColName.get(columnName)
	@deprecated("Renamed to .findColumnWithName(String)", "v1.21.1")
	def findColumnWithColumnName(columnName: String) = findColumnWithName(columnName)
	
	/**
	  * @param columnName Name of the targeted column, exactly as it appears in the database
	  * @throws NoSuchElementException If this table doesn't contain the specified column
	  * @return Targeted column
	  * @see [[findColumnWithName]]
	  */
	@throws[NoSuchElementException]("If this table doesn't contain the specified column")
	def columnWithName(columnName: String) = findColumnWithName(columnName).getOrElse {
		throw new NoSuchElementException(
			s"Table '$name' doesn't contain column named '$columnName'. Available columns: [${
				columns.view.map { _.columnName }.mkString(", ")}]")
	}
	@deprecated("Renamed to .columnWithName(String)", "v1.21.1")
	def columnWithColumnName(columnName: String) = columnWithName(columnName)
	
	/**
	  * Checks whether this table contains a matching column
	  */
	def contains(column: Column) = _columns.contains(column)
	/**
	  * checks whether this table contains a column matching the provided database model property name
	  */
	def contains(propertyName: String) = columnsByPropName.contains(propertyName.toLowerCase)
	
	/**
	  * Joins a new table, creating a new sql target.
	  * @param propertyName the name of a property matching a column in this table, which makes a
	  * reference to another table
	  */
	def joinFrom(propertyName: String, joinType: JoinType): SqlTarget = joinFrom(apply(propertyName), joinType)
	
	/**
	  * Adds a listener to this table so that it will be informed whenever this table is updated (see Triggers)
	  * @param listener A listener to add
	  */
	def addUpdateListener(listener: TableUpdateListener) = Triggers.addTableListener(this)(listener)
	
	/**
	  * Finds the first index from this table where specified condition is met
	  * @param where A search condition
	  * @param connection The database connection used
	  * @return The first index in this table that matches the specified condition
	  */
	@deprecated("Deprecated for removal at some point. It is recommended to use the more advanced database access interfaces", "v1.21.1")
	def index(where: Condition)(implicit connection: Connection) =
		connection(Select.index(this) + Where(where) + Limit(1)).firstIndexForTable(this)
	/**
	  * Finds indices in this table for rows where specified condition is met
	  * @param where A search condition
	  * @param connection The database connection used
	  * @return Indices in this table for rows matching the condition
	  */
	@deprecated("Deprecated for removal at some point. It is recommended to use the more advanced database access interfaces", "v1.21.1")
	def indices(where: Condition)(implicit connection: Connection) =
		connection(Select.index(this) + Where(where)).indicesForTable(this)
	/**
	  * Checks whether the specified index exists in this table in database
	  * @param index Searched index
	  * @param connection Database connection (implicit)
	  * @return Whether specified index exists in this table in the database
	  */
	@deprecated("Deprecated for removal at some point. It is recommended to use the more advanced database access interfaces", "v1.21.1")
	def containsIndex(index: Value)(implicit connection: Connection) = Exists.index(this, index)
	
	/**
	  * Checks whether the specified model contains all not-null properties defined in this table
	  * @param model A model to validate
	  * @return A validated copy of that model. Failure if the model didn't contain all required properties.
	  */
	def validate(model: HasProperties) = toModelDeclaration.validate(model)
}
