package utopia.vault.model.immutable.access

import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.sql.{Condition, MaxBy, MinBy, Select, Where}

/**
 * Used for reading singular ids from database
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 */
@deprecated("Replaced with utopia.vault.nosql.access.SingleModelAccess", "v1.4")
trait SingleIdAccess[+I] extends IdAccess[I]
{
	// OPERATORS	--------------------
	
	/**
	 * Reads an index from database
	 * @param condition Search condition
	 * @param connection Database connection (implicit)
	 * @return Index read from the database table that matches the specified condition
	 */
	def apply(condition: Condition)(implicit connection: Connection) =
	{
		val value = table.index(condition)
		if (value.isDefined)
			Some(valueToId(value))
		else
			None
	}
	
	
	// OTHER	------------------------
	
	/**
	 * Finds the index of a 'maximum' row
	 * @param orderColumn Column that determines row order
	 * @param connection DB connection (implicit)
	 * @return The index of the maximum row, based on specified ordering
	 */
	def max(orderColumn: Column)(implicit connection: Connection) = connection(Select.index(table) +
		MaxBy(orderColumn)).rows.headOption.map { r => valueToId(r.index) }
	
	/**
	 * Finds the index of a 'maximum' row
	 * @param orderPropertyName Name of property that determines row order
	 * @param connection DB connection (implicit)
	 * @return The index of the maximum row, based on specified ordering
	 */
	def max(orderPropertyName: String)(implicit connection: Connection): Option[I] = max(table(orderPropertyName))
	
	/**
	 * Finds index of a 'maximum' row
	 * @param condition Search condition
	 * @param orderColumn Column that determines ordering
	 * @param connection Database connection (implicit)
	 * @return The index of the 'maximum' row
	 */
	def max(condition: Condition, orderColumn: Column)(implicit connection: Connection) =
		connection(Select.index(table) + Where(condition) + MaxBy(orderColumn)).rows.headOption.map { r => valueToId(r.index) }
	
	/**
	 * Finds index of a 'maximum' row
	 * @param condition Search condition
	 * @param orderProperty Name of property that determines ordering
	 * @param connection Database connection (implicit)
	 * @return The index of the 'maximum' row
	 */
	def max(condition: Condition, orderProperty: String)(implicit connection: Connection): Option[I] =
		max(condition, table(orderProperty))
	
	/**
	 * Finds the index of a 'minimum' row
	 * @param orderColumn Column that determines row order
	 * @param connection DB connection (implicit)
	 * @return The index of the minimum row, based on specified ordering
	 */
	def min(orderColumn: Column)(implicit connection: Connection) = connection(Select.index(table) +
		MinBy(orderColumn)).rows.headOption.map { r => valueToId(r.index) }
	
	/**
	 * Finds the index of a 'minimum' row
	 * @param orderPropertyName Name of property that determines row order
	 * @param connection DB connection (implicit)
	 * @return The index of the minimum row, based on specified ordering
	 */
	def min(orderPropertyName: String)(implicit connection: Connection): Option[I] = min(table(orderPropertyName))
	
	/**
	 * Finds index of a 'minimum' row
	 * @param condition Search condition
	 * @param orderColumn Column that determines ordering
	 * @param connection Database connection (implicit)
	 * @return The index of the 'minimum' row
	 */
	def min(condition: Condition, orderColumn: Column)(implicit connection: Connection) =
		connection(Select.index(table) + Where(condition) + MinBy(orderColumn)).rows.headOption.map { r => valueToId(r.index) }
	
	/**
	 * Finds index of a 'minimum' row
	 * @param condition Search condition
	 * @param orderProperty Name of property that determines ordering
	 * @param connection Database connection (implicit)
	 * @return The index of the 'minimum' row
	 */
	def min(condition: Condition, orderProperty: String)(implicit connection: Connection): Option[I] =
		min(condition, table(orderProperty))
}