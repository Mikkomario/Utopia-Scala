package utopia.trove.database

import utopia.flow.util.Version
import utopia.trove.model.partial.DatabaseVersionData
import utopia.trove.model.stored.DatabaseVersion
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.access.many.model.ManyRowModelAccess

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
	
	override def accessCondition = None
	
	
	// OTHER	---------------------------
	
	/**
	  * Inserts a new database version to the DB
	  * @param versionNumber New version number
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted version model
	  */
	def insert(versionNumber: Version)(implicit connection: Connection) =
		model.insert(DatabaseVersionData(versionNumber))
	
	/**
	  * Inserts a number of database version recordings to DB
	  * @param versions Versions to import
	  * @param connection DB Connection (implicit)
	  * @return Newly generated row ids
	  */
	def insert(versions: Seq[DatabaseVersionData])(implicit connection: Connection) = model.insert(versions)
}
