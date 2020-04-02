package utopia.vault.model.immutable

import utopia.flow.datastructure.immutable.{ModelDeclaration, Value}
import utopia.vault.database.Connection
import utopia.vault.nosql.factory.StorableFactory
import utopia.vault.sql.JoinType.JoinType
import utopia.vault.sql.{Condition, Exists, Limit, Select, SqlSegment, SqlTarget, Where}

import scala.collection.immutable.HashSet

/**
 * A table represents a table in the database. Each table has a set of columns, one of which is
 * usually the primary column. Tables may reference other tables through columns too.
 * @author Mikko Hilpinen
 * @since 9.3.2017
  * @param name This table's name in the database
  * @param databaseName The name of the database that contains this table
  * @param columns The columns that belong to this table
 */
case class Table(name: String, databaseName: String, columns: Vector[Column]) extends SqlTarget
{
    // ATTRIBUTES    ---------------------------
    
    /**
      * The primary column for this table. Not all tables do have primary columns though.
      */
    val primaryColumn = columns.find { _.isPrimary }
    
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
    def toFactory = StorableFactory(this)
    
    /**
      * @param connection Database connection
      * @return All defined indices for this table in database
      */
    def allIndices(implicit connection: Connection) = Select.index(this).execute().indicesForTable(this)
    
    
    // OPERATORS    ----------------------------
    
    /**
     * Finds a column with the provided property name associated with it. If you are unsure whether
     * such a column exists in the table, use find instead
     */
    // FIXME: Add a more clear thrown exception
    def apply(propertyName: String) = find(propertyName).get
    
    /**
     * Finds the columns matching the provided property names
     */
    def apply(propertyNames: Iterable[String]) = columns.filter {
            column => propertyNames.exists { _ == column.name } }
    
    /**
     * Finds columns matching the provided property names
     */
    def apply(propertyName: String, second: String, others: String*): Vector[Column] = apply(Vector(propertyName, second) ++ others)
    
    
    // OTHER METHODS    ------------------------
    
    /**
     * Finds a column with the provided property name. Returns None if no such column exists in
     * this table
     */
    def find(propertyName: String) = columns.find { _.name == propertyName }
    
    /**
     * Finds a column with the specified column name. Returns None if no such column exists.
     */
    def findColumnWithColumnName(columnName: String) = columns.find { _.columnName == columnName }
    
    /**
     * Finds a column with the specified column name. If you are unsure whether such a column
     * exists, please used findColumnWithColumnName instead
     */
    def columnWithColumnName(columnName: String) = findColumnWithColumnName(columnName).get
    
    /**
     * Checks whether this table contains a matching column
     */
    def contains(column: Column) = columns.contains(column)
    
    /**
     * checks whether this table contains a column matching the provided property name
     */
    def contains(propertyName: String) = columns.exists { _.name == propertyName }
    
    /**
     * Joins a new table, creating a new sql target.
     * @param propertyName the name of a property matching a column in this table, which makes a
     * reference to another table
     */
    def joinFrom(propertyName: String, joinType: JoinType): SqlTarget = joinFrom(apply(propertyName), joinType)

    /**
      * Finds the first index from this table where specified condition is met
      * @param where A search condition
      * @param connection The database connection used
      * @return The first index in this table that matches the specified condition
      */
    def index(where: Condition)(implicit connection: Connection) =
    {
        connection(Select.index(this) + Where(where) + Limit(1)).firstIndexForTable(this)
    }

    /**
      * Finds indices in this table for rows where specified condition is met
      * @param where A search condition
      * @param connection The database connection used
      * @return Indices in this table for rows matching the condition
      */
    def indices(where: Condition)(implicit connection: Connection) =
    {
        connection(Select.index(this) + Where(where)).indicesForTable(this)
    }
    
    /**
     * Checks whether the specified index exists in this table in database
     * @param index Searched index
     * @param connection Database connection (implicit)
     * @return Whether specified index exists in this table in the database
     */
    def containsIndex(index: Value)(implicit connection: Connection) = Exists.index(this, index)
}
