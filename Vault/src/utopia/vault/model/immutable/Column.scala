package utopia.vault.model.immutable

import utopia.flow.datastructure.immutable.{PropertyDeclaration, Value}
import utopia.flow.generic.DataType
import utopia.vault.model.enumeration.ComparisonOperator
import utopia.vault.sql.{Condition, ConditionElement, SqlSegment}

/**
 * Columns represent database columns and can be used as templates for different properties
 * @author Mikko Hilpinen
 * @since 8.3.2017
 */
case class Column(propertyName: String, columnName: String, tableName: String, override val dataType: DataType,
                  allowsNull: Boolean = true, override val defaultValue: Option[Value] = None,
                  isPrimary: Boolean = false, usesAutoIncrement: Boolean = false)
        extends PropertyDeclaration with ConditionElement
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
