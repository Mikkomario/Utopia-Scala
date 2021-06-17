package utopia.vault.nosql.access

import scala.language.implicitConversions
import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Condition, Update, Where}

/**
 * A common trait for model accessors which are able to target a distinct value / values with pull and put
 * method. These accessors also support auto-access to their values.
 * @author Mikko Hilpinen
 * @since 6.4.2021, v1.7
 */
trait DistinctModelAccess[+M, +A, +V] extends DistinctReadModelAccess[M, A, V]
{
	/**
	 * Updates value / values of an individual column accessible through this accessor
	 * @param column Column to update
	 * @param value Value to assign to the column
	 * @param updateCondition An additional condition that must be fulfilled in order to apply the update (optional)
	 * @param connection DB Connection (implicit)
	 * @return Whether any row was updated
	 */
	def putColumn(column: Column, value: Value, updateCondition: Option[Condition] = None)
	             (implicit connection: Connection) =
		connection(Update(target, column, value) + mergeCondition(updateCondition).map { Where(_) }).updatedRows
	
	/**
	 * Updates value / values of an individual attribute / column in this model
	 * @param attributeName Name of the attribute / property which is updated
	 * @param value Value to assign to the attribute
	 * @param updateCondition An additional condition that must be fulfilled in order to apply the update (optional)
	 * @param connection DB Connection (implicit)
	 * @return Whether any row was updated
	 */
	def putAttribute(attributeName: String, value: Value, updateCondition: Option[Condition] = None)
	                (implicit connection: Connection) =
		putColumn(table(attributeName), value, updateCondition)
}
