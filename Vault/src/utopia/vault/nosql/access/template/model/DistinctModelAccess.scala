package utopia.vault.nosql.access.template.model

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Condition, Update, Where}

/**
  * A common trait for model accessors which are able to target a distinct value / values with pull and put
  * method. These accessors also support auto-access to their values.
  * @author Mikko Hilpinen
  * @since 6.4.2021, v1.7
  * @tparam M Type of model returned
  * @tparam A The format in which model data is returned (E.g. a list of models)
  * @tparam V Format in which column values are returned (E.g. A single value or a vector of values)
  */
trait DistinctModelAccess[+M, +A, +V] extends DistinctReadModelAccess[M, A, V]
{
	/**
	  * Updates value / values of an individual column accessible through this accessor
	  * @param column          Column to update
	  * @param value           Value to assign to the column
	  * @param updateCondition An additional condition that must be fulfilled in order to apply the update (optional)
	  * @param connection      DB Connection (implicit)
	  * @return Whether any row was updated
	  */
	def putColumn(column: Column, value: Value, updateCondition: Option[Condition] = None)
	             (implicit connection: Connection) =
		connection(Update(target, column, value) + mergeCondition(updateCondition).map { Where(_) }).updatedRows
	/**
	  * Updates value / values of an individual attribute / column in this model
	  * @param propertyName   Name of the attribute / property which is updated
	  * @param value           Value to assign to the attribute
	  * @param updateCondition An additional condition that must be fulfilled in order to apply the update (optional)
	  * @param connection      DB Connection (implicit)
	  * @return Whether any row was updated
	  */
	def putProperty(propertyName: String, value: Value, updateCondition: Option[Condition] = None)
	               (implicit connection: Connection) =
		putColumn(table(propertyName), value, updateCondition)
	@deprecated("Renamed to .putProperty", "v1.17")
	def putAttribute(attributeName: String, value: Value, updateCondition: Option[Condition] = None)
	                (implicit connection: Connection) =
		putProperty(attributeName, value, updateCondition)
	
	/**
	  * Sets a NULL value to a column of all accessible items
	  * @param column Targeted column
	  * @param clearCondition a condition that must be met in order for the update to occur (optional)
	  * @param connection Implicit DB connection
	  * @return Whether any row was targeted
	  */
	def clearColumn(column: Column, clearCondition: Option[Condition] = None)(implicit connection: Connection) =
		putColumn(column, Value.empty, clearCondition)
	/**
	  * Sets a NULL value to a column of all accessible items
	  * @param propertyName         The property name of the targeted column
	  * @param clearCondition a condition that must be met in order for the update to occur (optional)
	  * @param connection     Implicit DB connection
	  * @return Whether any row was targeted
	  */
	def clearProperty(propertyName: String, clearCondition: Option[Condition] = None)(implicit connection: Connection) =
		putProperty(propertyName, Value.empty, clearCondition)
}
