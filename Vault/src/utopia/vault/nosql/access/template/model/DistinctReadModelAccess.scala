package utopia.vault.nosql.access.template.model

import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable

import scala.language.implicitConversions

/**
 * A common trait for model accessors which are able to target a distinct value / values with the pull method.
 * These accessors also support auto-access to their values.
 * @author Mikko Hilpinen
 * @since 6.4.2021, v1.8
  * @tparam M Type of model returned
  * @tparam A The format in which model data is returned (E.g. a list of models)
  * @tparam V Format in which column values are returned (E.g. A single value or a vector of values)
 */
trait DistinctReadModelAccess[+M, +A, +V] extends ModelAccess[M, A, V]
{
	// COMPUTED -----------------------------
	
	/**
	 * @param connection Implicit database connection
	 * @return The unique item accessed through this access point. None if no item was found.
	 */
	def pull(implicit connection: Connection) = read(accessCondition)
	
	
	// OTHER    -----------------------------
	
	/**
	 * Reads all accessible values of a column
	 * @param column Targeted column
	  * @param joins Joins to apply to this query (default = empty)
	 * @param connection DB Connection (implicit)
	 * @return All accessible values of that column. May contain empty values.
	 */
	def pullColumn(column: Column, joins: Joinable*)(implicit connection: Connection) =
		readColumn(column, joins = joins)
	/**
	 * Reads all accessible values of a column / attribute
	 * @param attributeName Name of the targeted attribute
	 * @param connection DB Connection (implicit)
	 * @return All accessible values of that column / attribute. May contain empty values.
	 */
	// TODO: Rename to pullProperty
	def pullAttribute(attributeName: String)(implicit connection: Connection) = pullColumn(table(attributeName))
}
