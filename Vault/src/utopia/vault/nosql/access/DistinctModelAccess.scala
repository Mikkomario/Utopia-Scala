package utopia.vault.nosql.access

import scala.language.implicitConversions
import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Exists, Update, Where}

object DistinctModelAccess
{
	// Automatically accesses results of .pull
	implicit def autoAccess[A](accessor: DistinctModelAccess[_, A, _])
	                          (implicit connection: Connection): A = accessor.pull
}

/**
 * A common trait for model accessors which are able to target a distinct value / values with pull and put
 * method. These accessors also support auto-access to their values.
 * @author Mikko Hilpinen
 * @since 6.4.2021, v1.7
 */
trait DistinctModelAccess[+M, +A, +V] extends ModelAccess[M, A, V]
{
	/**
	 * @param connection Implicit database connection
	 * @return The unique item accessed through this access point. None if no item was found.
	 */
	def pull(implicit connection: Connection) = read(globalCondition)
	
	/**
	 * @param connection DB Connection (implicit)
	 * @return Whether there exists an item accessible from this access point
	 */
	def nonEmpty(implicit connection: Connection) = globalCondition match
	{
		case Some(condition) => Exists(target, condition)
		case None => Exists.any(target)
	}
	
	/**
	 * @param connection DB Connection (implicit)
	 * @return Whether there doesn't exist a single row accessible from this access point
	 */
	def isEmpty(implicit connection: Connection) = !nonEmpty
	
	/**
	 * Reads all accessible values of a column
	 * @param column Targeted column
	 * @param connection DB Connection (implicit)
	 * @return All accessible values of that column. May contain empty values.
	 */
	def pullColumn(column: Column)(implicit connection: Connection) = readColumn(column)
	
	/**
	 * Reads all accessible values of a column / attribute
	 * @param attributeName Name of the targeted attribute
	 * @param connection DB Connection (implicit)
	 * @return All accessible values of that column / attribute. May contain empty values.
	 */
	def pullAttribute(attributeName: String)(implicit connection: Connection) =
		pullColumn(table(attributeName))
	
	/**
	 * Updates value / values of an individual column accessible through this accessor
	 * @param column Column to update
	 * @param value Value to assign to the column
	 * @param connection DB Connection (implicit)
	 * @return Whether any row was updated
	 */
	def putColumn(column: Column, value: Value)(implicit connection: Connection) =
		connection(Update(target, column, value) + globalCondition.map { Where(_) }).updatedRows
	
	/**
	 * Updates value / values of an individual attribute / column in this model
	 * @param attributeName Name of the attribute / property which is updated
	 * @param value Value to assign to the attribute
	 * @param connection DB Connection (implicit)
	 * @return Whether any row was updated
	 */
	def putAttribute(attributeName: String, value: Value)(implicit connection: Connection) =
		putColumn(table(attributeName), value)
}
