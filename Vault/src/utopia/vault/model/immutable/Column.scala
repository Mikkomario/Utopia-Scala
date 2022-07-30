package utopia.vault.model.immutable

import utopia.flow.datastructure.immutable.{PropertyDeclaration, Value}
import utopia.flow.generic.DataType
import utopia.vault.database.References
import utopia.vault.model.error.{ColumnNotFoundException, NoReferenceFoundException}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.{Condition, ConditionElement, Join, JoinType, SqlSegment}

import scala.util.{Failure, Success}

/**
 * Columns represent database columns and can be used as templates for different properties
 * @author Mikko Hilpinen
 * @since 8.3.2017
  * @param propertyName Name of the database model property represented by this column
  * @param columnName Name of this column as it appears in the database
  * @param tableName Name of the table where this column belongs
  * @param dataType Data type accepted by this column
  * @param defaultValue Default value of this column, if applicable
  * @param allowsNull Whether this column allows a null value (default = true)
  * @param isPrimary Whether this column represents a primary key
  * @param usesAutoIncrement Whether this column uses auto-increment
 */
case class Column(propertyName: String, columnName: String, tableName: String, override val dataType: DataType,
                  override val defaultValue: Option[Value] = None,
                  allowsNull: Boolean = true, isPrimary: Boolean = false, usesAutoIncrement: Boolean = false)
        extends PropertyDeclaration with ConditionElement with Joinable
{
    // COMPUTED PROPERTIES    ------------------
    
    /**
     * Whether a value is required in this column when data is inserted to the database
     */
    def isRequiredInInsert = !allowsNull && !usesAutoIncrement && defaultValue.isEmpty

    /**
      * @return The name of this column surrounded by `backticks`
      */
    def sqlColumnName = s"`$columnName`"
    /**
     * Returns the name of this column, including table name for disambiguity. Also includes backticks.
     */
    def columnNameWithTable = s"`$tableName`.`$columnName`"
    
    /**
     * Creates a condition that checks whether the column value in the database is null
     */
    def isNull = Condition(SqlSegment(columnNameWithTable + " IS NULL"))
    /**
     * Creates a condition that checks whether the column value in the database is not null
     */
    def isNotNull = Condition(SqlSegment(columnNameWithTable + " IS NOT NULL"))
    
    
    // IMPLEMENTED  -----------------------------
    
    def name = propertyName
    
    override def toString = s"$columnName $dataType ${ if (isPrimary) "PRIMARY KEY " else ""} ${
        if (usesAutoIncrement) "AUTO_INCREMENT " else ""}"
    
    override def toSqlSegment = SqlSegment(columnNameWithTable)
    
    override def toJoinFrom(originTables: Vector[Table], joinType: JoinType) = {
        originTables.find { _.contains(this) } match {
            case Some(table) =>
                References.from(table, this) match {
                    case Some(target) => Success(Join(this, target.table, target.column, joinType))
                    case None => Failure(new NoReferenceFoundException(
                        s"$columnNameWithTable doesn't refer to any table"))
                }
            case None => Failure(new ColumnNotFoundException(s"None of tables ${
                originTables.map { _.name }.mkString(", ") } contains column $columnNameWithTable"))
        }
    }
    
    
    // OPERATORS    ----------------------------
    
    /**
     * Creates an equality condition between a column and a specified value. This condition can
     * be then used in a sql statement. Calling this with an empty value is same as calling isNull
     */
    def <=>(value: Value) = if (value.isEmpty) isNull else makeCondition("<=>", value)
    /**
     * Creates a not equals condition between a column and a specified value. This condition can
     * be used in a sql statement. Calling this with an empty value is same as calling isNotNull
     */
    def <>(value: Value) = if (value.isEmpty) isNotNull else makeCondition("<>", value)
    
    
    // OTHER METHODS    ---------------------
    
    private def makeCondition(operator: String, value: Value) = Condition(SqlSegment(
        s"$columnNameWithTable $operator ?", Vector(value)))
}
