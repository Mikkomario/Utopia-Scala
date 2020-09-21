package utopia.trove.database

import utopia.trove.model.VersionNumber
import utopia.trove.model.partial.DatabaseVersionData
import utopia.trove.model.stored.DatabaseVersion
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.access.ManyRowModelAccess

/**
  * Used for accessing multiple database versions at a time
  * @author Mikko Hilpinen
  * @since 21.9.2020, v1
  */
case class DbDatabaseVersions(versionTable: Table) extends ManyRowModelAccess[DatabaseVersion]
{
	// ATTRIBUTES	-----------------------
	
	override val factory = DatabaseVersionFactory(versionTable)
	
	private val model = DatabaseVersionModel(factory)
	
	
	// IMPLEMENTED	-----------------------
	
	override def globalCondition = None
	
	
	// OTHER	---------------------------
	
	/**
	  * Inserts a new database version to the DB
	  * @param versionNumber New version number
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted version model
	  */
	def insert(versionNumber: VersionNumber)(implicit connection: Connection) =
		model.insert(DatabaseVersionData(versionNumber))
}
