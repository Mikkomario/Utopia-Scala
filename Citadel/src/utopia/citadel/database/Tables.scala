package utopia.citadel.database

import utopia.vault.model.immutable.Table

/**
  * Used for accessing various tables in the Utopia Citadel project and all dependent projects
  * @author Mikko Hilpinen
  * @since 26.6.2021 v1.0
  */
object Tables
{
	import utopia.citadel.util.CitadelContext._
	
	// ATTRIBUTES	----------------------
	
	private lazy val access = new utopia.vault.database.Tables(connectionPool)
	
	
	// OTHER	-------------------------------
	
	/**
	 * @param databaseName Name of the used database
	 * @param tableName Name of the targeted table
	 * @return A cached table
	 */
	def apply(databaseName: String, tableName: String): Table = access(databaseName, tableName)
	
	/**
	  * @param tableName Name of targeted table
	  * @return a cached table
	  */
	def apply(tableName: String): Table = access(databaseName, tableName)
}
