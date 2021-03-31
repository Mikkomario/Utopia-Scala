package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Update, Where}

/**
 * Common trait for access points which target an individual and unique model.
 * E.g. When targeting a model based on the primary row id
 * @author Mikko Hilpinen
 * @since 31.3.2021, v1.6.1
 */
trait UniqueModelAccess[+A] extends SingleModelAccess[A] with UniqueAccess[A]
{
	/**
	 * @param connection Database connection (implicit)
	 * @return The index of this model in database (may be empty)
	 */
	def index(implicit connection: Connection) = table.primaryColumn match
	{
		case Some(primaryColumn) => pullColumn(primaryColumn)
		case None => Value.empty
	}
	
	/**
	 * Reads the value of an individual column in this model
	 * @param column Column to read
	 * @param connection DB Connection (implicit)
	 * @return value of that column (may be empty)
	 */
	def pullColumn(column: Column)(implicit connection: Connection) = readColumn(column)
	
	/**
	 * Reads the value of an individual attribute / column in this model
	 * @param attributeName Name of the attribute to read
	 * @param connection DB Connection (implicit)
	 * @return value of that attribute (may be empty)
	 */
	def pullAttribute(attributeName: String)(implicit connection: Connection) =
		pullColumn(table(attributeName))
	
	/**
	 * Updates the value of an individual column in this model
	 * @param column Column to update
	 * @param value Value to assign to the column
	 * @param connection DB Connection (implicit)
	 * @return Whether any row was updated
	 */
	def putColumn(column: Column, value: Value)(implicit connection: Connection) =
		connection(Update(target, column, value) + Where(condition)).updatedRows
	
	/**
	 * Updates the value of an individual attribute / column in this model
	 * @param attributeName Name of the attribute / property which is updated
	 * @param value Value to assign to the attribute
	 * @param connection DB Connection (implicit)
	 * @return Whether any row was updated
	 */
	def putAttribute(attributeName: String, value: Value)(implicit connection: Connection) =
		putColumn(table(attributeName), value)
}
