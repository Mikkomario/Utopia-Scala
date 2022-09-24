package utopia.vault.model.immutable

import utopia.flow.collection.immutable.Pair
import utopia.flow.datastructure.template.Model
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.{Model, Property}
import utopia.flow.util.CollectionExtensions._
import utopia.vault.database.{Connection, References, TableUpdateListener, Triggers}
import utopia.vault.model.error.NoReferenceFoundException
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.factory.row.model.FromRowModelFactory
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, Exists, Join, JoinType, Limit, Select, SqlSegment, SqlTarget, Where}

import scala.collection.immutable.HashSet
import scala.util.{Failure, Success}

/**
  * A table represents a table in the database. Each table has a set of columns, one of which is
  * usually the primary column. Tables may reference other tables through columns too.
  * @author Mikko Hilpinen
  * @since 9.3.2017
  * @param name This table's name in the database
  * @param databaseName The name of the database that contains this table
  * @param columns The columns that belong to this table
  */
case class Table(name: String, databaseName: String, columns: Vector[Column]) extends SqlTarget with Joinable
{
	// ATTRIBUTES    ---------------------------
	
	/**
	  * The primary column for this table. Not all tables do have primary columns though.
	  */
	val primaryColumn = columns.find { _.isPrimary }
	/**
	  * A map that contains all the columns of this table with their lower case property names as keys
	  */
	private lazy val columnsByAttName = columns.map { c => c.name.toLowerCase -> c }.toMap
	
	/**
	  * A model declaration based on this table
	  */
	lazy val toModelDeclaration = ModelDeclaration(columns)
	/**
	  * A model declaration based on the required (not null) columns in this table
	  */
	lazy val requirementDeclaration = ModelDeclaration(columns.filterNot { _.allowsNull })
	
	
	// COMPUTED PROPERTIES    ------------------
	
	override def toString = name
	
	override def toSqlSegment = SqlSegment(sqlName, Vector(), Some(databaseName), HashSet(this))
	
	/**
	  * @return The name of this table in sql format (same as original name but surrounded with `backticks`). If this
	  *         table has an alias, includes that.
	  */
	def sqlName = s"`$name`"
	
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
	  * @param connection Database connection
	  * @return All defined indices for this table in database
	  */
	def allIndices(implicit connection: Connection) =
		Select.index(this).execute().indicesForTable(this)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def tables = Vector(this)
	
	override def toJoinFrom(originTables: Vector[Table], joinType: JoinType = Inner) = {
		// Finds the first table referencing (or being referenced by) the provided table and uses
		// that for a join
		originTables.findMap { left => References.connectionBetween(left, this) } match {
			case Some(Pair(leftColumn, rightColumn)) => Success(Join(leftColumn, this, rightColumn, joinType))
			case None =>
				Failure(new NoReferenceFoundException(
					s"Cannot find a reference between ${
						originTables.map { _.name }.mkString(" + ") } and $name. Only found references: [${
						(tables :+ this).flatMap(References.from).mkString(", ")}]"))
		}
	}
	
	
	// OPERATORS    ----------------------------
	
	/**
	  * Finds a column with the provided property name associated with it. If you are unsure whether
	  * such a column exists in the table, use find instead
	  */
	def apply(propertyName: String) = find(propertyName).getOrElse {
		throw new NoSuchElementException(
			s"Table '$name' doesn't contain property '$propertyName'. Available properties: [${
				columns.map { _.name }.mkString(", ")}]")
	}
	
	/**
	  * Finds the columns matching the provided property names
	  */
	def apply(propertyNames: Iterable[String]) =
		columns.filter { column => propertyNames.exists { _ == column.name } }
	/**
	  * Finds columns matching the provided property names
	  */
	def apply(propertyName: String, second: String, others: String*): Vector[Column] =
		apply(Vector(propertyName, second) ++ others)
	
	
	// OTHER METHODS    ------------------------
	
	/**
	  * Finds a column with the provided database model property name. Returns None if no such column exists in
	  * this table
	  */
	def find(propertyName: String) = columnsByAttName.get(propertyName.toLowerCase)
	
	/**
	  * Finds a column with the specified column name. Returns None if no such column exists.
	  */
	def findColumnWithColumnName(columnName: String) = columns.find { _.columnName == columnName }
	
	/**
	  * Finds a column with the specified column name. If you are unsure whether such a column
	  * exists, please used findColumnWithColumnName instead
	  */
	def columnWithColumnName(columnName: String) = findColumnWithColumnName(columnName) match
	{
		case Some(column) => column
		case None => throw new NoSuchElementException(
			s"Table '$name' doesn't contain column named '$columnName'. Available columns: [${
				columns.map { _.columnName }.mkString(", ")}]")
	}
	
	/**
	  * Checks whether this table contains a matching column
	  */
	def contains(column: Column) = columns.contains(column)
	/**
	  * checks whether this table contains a column matching the provided database model property name
	  */
	def contains(propertyName: String) = columnsByAttName.contains(propertyName.toLowerCase)
	
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
	def index(where: Condition)(implicit connection: Connection) =
		connection(Select.index(this) + Where(where) + Limit(1)).firstIndexForTable(this)
	
	/**
	  * Finds indices in this table for rows where specified condition is met
	  * @param where A search condition
	  * @param connection The database connection used
	  * @return Indices in this table for rows matching the condition
	  */
	def indices(where: Condition)(implicit connection: Connection) =
		connection(Select.index(this) + Where(where)).indicesForTable(this)
	
	/**
	  * Checks whether the specified index exists in this table in database
	  * @param index Searched index
	  * @param connection Database connection (implicit)
	  * @return Whether specified index exists in this table in the database
	  */
	def containsIndex(index: Value)(implicit connection: Connection) = Exists.index(this, index)
	
	/**
	  * Checks whether the specified model contains all not-null properties defined in this table
	  * @param model A model to validate
	  * @return A validated copy of that model. Failure if the model didn't contain all required properties.
	  */
	def validate(model: Model[Property]) = requirementDeclaration.validate(model).toTry
}
