package utopia.keep.util

import utopia.flow.generic.{DataType, EnvironmentNotSetupException}
import utopia.vault.database.Tables

/**
  * A context that must be set up before using Keep features
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object KeepContext
{
	// ATTRIBUTES   ----------------------------
	
	private var data: Option[Data] = None
	
	
	// COMPUTED --------------------------------
	
	private def initializedData =
		data.getOrElse { throw EnvironmentNotSetupException(
			"KeepContext.setup() must be called before using Keep features") }
	
	/**
	  * @return The global tables instance
	  */
	def tables = initializedData.tables
	/**
	  * @return Name of the database that holds Keep tables
	  */
	def databaseName = initializedData.databaseName
	
	
	// OTHER    --------------------------------
	
	def setup(tables: Tables, databaseName: String) = {
		DataType.setup()
		data = Some(Data(tables, databaseName))
	}
	
	
	// NESTED   --------------------------------
	
	private case class Data(tables: Tables, databaseName: String)
}
