package utopia.vault.sql

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Table

/**
  * Used for checking whether any rows / data exists for a specific query / condition
  * @author Mikko Hilpinen
  * @since 10.7.2019, v1.2+
  */
object Exists
{
	/**
	  * Checks whether the specified query yields any results
	  * @param target Targeted table(s)
	  * @param where A condition applied to the query
	  * @param connection Database connection used (implicit)
	  * @return Whether specified query yielded any results
	  */
	def apply(target: SqlTarget, where: Condition)(implicit connection: Connection) =
		connection(Select.nothing(target) + Where(where) + Limit(1)).nonEmpty
	
	/**
	  * Checks whether the specified table contains specified index
	  * @param table A table
	  * @param index Searched index
	  * @param connection Database connection (implicit)
	  * @return Whether the table contains the specified index
	  */
	def index(table: Table, index: Value)(implicit connection: Connection) = {
		if (table.hasPrimaryColumn)
			apply(table, table.primaryColumn.get <=> index)
		else
			false
	}
	
	/**
	  * Checks whether there exist any rows in the specified target
	  * @param target Targeted table(s)
	  * @param connection DB Connection (implicit)
	  * @return Whether the target contains any rows
	  */
	def any(target: SqlTarget)(implicit connection: Connection) =
		connection(Select.nothing(target) + Limit(1)).nonEmpty
}
