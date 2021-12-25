package utopia.vault.database.columnlength

import utopia.flow.async.Volatile

/**
  * Used for tracking length limits applying to each database column
  * @author Mikko Hilpinen
  * @since 25.12.2021, v1.12
  */
object ColumnLengthLimits
{
	// ATTRIBUTES   ------------------------------
	
	// (database name, table name, column name) => column length limit
	private val limits = Volatile(Map[(String, String, String), ColumnLengthLimit]())
	// private val limits2 = Volatile(Ma)
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param databaseName Name of targeted database
	  * @param tableName Name of targeted table
	  * @param columnName Name of targeted column (not property)
	  * @return A length limit to apply to that column, if specified
	  */
	def apply(databaseName: String, tableName: String, columnName: String) =
		limits.value.get((databaseName, tableName, columnName))
	/**
	  * @param key A key consisting of database name, table name and column name
	  * @param limit A limit to apply to that column
	  */
	def update(key: (String, String, String), limit: ColumnLengthLimit) = limits.update { _ + (key -> limit) }
}
