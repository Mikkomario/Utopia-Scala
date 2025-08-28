package utopia.vault.model.immutable

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.model.immutable.{PropertyDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType
import utopia.vault.model.error.ColumnNotFoundException
import utopia.vault.model.template.Joinable
import utopia.vault.sql.{Condition, ConditionElement, JoinType, SqlSegment}

import scala.util.Failure

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
                  override val defaultValue: Value = Value.empty, allowsNull: Boolean = true,
                  isPrimary: Boolean = false, usesAutoIncrement: Boolean = false)
	extends PropertyDeclaration with ConditionElement with Joinable
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * Name of this column, as it should appear in SQL statements.
	  * Prefixed by the name of this column's table, and properly escaped.
	  */
	lazy val sqlName = s"`$tableName`.`$columnName`"
	
	/**
	  * Creates a condition that checks whether the column value in the database is null
	  */
	lazy val isNull = Condition(SqlSegment(s"$sqlName IS NULL"))
	/**
	  * Creates a condition that checks whether the column value in the database is not null
	  */
	lazy val isNotNull = Condition(SqlSegment(s"$sqlName IS NOT NULL"))
	
	
	// COMPUTED PROPERTIES    ------------------
	
	/**
	  * @return A description of this column, as it would appear in a create table or alter table -statement.
	  */
	def sqlDescription = s"$columnName $dataType ${ if (isPrimary) "PRIMARY KEY " else ""}${
		if (usesAutoIncrement) "AUTO_INCREMENT " else ""}"
	
	/**
	  * Whether a value is required in this column when data is inserted to the database
	  */
	def isRequiredInInsert = !allowsNull && !usesAutoIncrement && defaultValue.isEmpty
	
	/**
	  * @return Name of this column, as it should appear in SQL statements involving only a single table.
	  *         Escaped in order to avoid naming conflicts.
	  */
	def shortSqlName = s"`$columnName`"
	@deprecated("Renamed to shortSqlName", "v1.21.1")
	def sqlColumnName = shortSqlName
	/**
	  * Returns the name of this column, including table name for disambiguity. Also includes backticks.
	  */
	@deprecated("Please use .sqlName instead", "v1.21.1")
	def columnNameWithTable = sqlName
	
	
	// IMPLEMENTED  -----------------------------
	
	override def name = propertyName
	override def alternativeNames = Single(columnName)
	
	override def isOptional = allowsNull
	
	override def toString = sqlName
	override def toSqlSegment = SqlSegment(sqlName)
	
	override def toJoinsFrom(originTables: Seq[Table], joinType: JoinType) = {
		// Checks which of the origin tables contains this column
		originTables.find { _.contains(this) } match {
			case Some(table) => TableColumn(table, this).toJoinsFrom(originTables, joinType)
			case None =>
				Failure(new ColumnNotFoundException(s"None of tables ${
					originTables.map { _.name }.mkString(", ") } contains column $sqlName"))
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
	
	private def makeCondition(operator: String, value: Value) =
		Condition(SqlSegment(s"$sqlName $operator ?", Single(value)))
}
