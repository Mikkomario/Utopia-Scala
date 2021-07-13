package utopia.trove.database

import utopia.trove.model.stored.DatabaseVersion
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.access.single.model.SingleRowModelAccess

/**
  * Used for accessing individual database versions in the DB
  * @author Mikko Hilpinen
  * @since 21.9.2020, v1
  */
case class DbDatabaseVersion(versionTable: Table) extends SingleRowModelAccess[DatabaseVersion]
{
	// IMPLEMENTED	-----------------------
	
	override val factory = DatabaseVersionFactory(versionTable)
	
	override def globalCondition = None
	
	
	// COMPUTED	---------------------------
	
	/**
	  * @param connection Database connection (implicit)
	  * @return Latest database version recording. None if no versions have been recorded yet.
	  */
	def latest(implicit connection: Connection) = factory.latest
}
