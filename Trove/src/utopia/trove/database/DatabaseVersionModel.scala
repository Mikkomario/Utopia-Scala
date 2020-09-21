package utopia.trove.database

import java.time.Instant

import utopia.flow.generic.ValueConversions._
import utopia.trove.model.VersionNumber
import utopia.trove.model.partial.DatabaseVersionData
import utopia.trove.model.stored.DatabaseVersion
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object DatabaseVersionModel
{
	/**
	  * @param versionFactory A database version factory
	  * @return A database version model factory
	  */
	def apply(versionFactory: DatabaseVersionFactory) = DatabaseVersionModelFactory(versionFactory)
}

case class DatabaseVersionModelFactory(versionFactory: DatabaseVersionFactory)
{
	/**
	  * @param id Version row id (optional)
	  * @param number Version number (optional)
	  * @param created Version creation time (optional)
	  * @return A new version model
	  */
	def apply(id: Option[Int] = None, number: Option[VersionNumber] = None, created: Option[Instant] = None) =
		DatabaseVersionModel(versionFactory, id, number, created)
	
	/**
	  * Inserts a new database version to the database
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted version
	  */
	def insert(data: DatabaseVersionData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(data.number), Some(data.created)).insert().getInt
		DatabaseVersion(id, data)
	}
}

/**
  * Used for interacting with database version data in DB
  * @author Mikko Hilpinen
  * @since 28.7.2020, v1.2
  */
case class DatabaseVersionModel(factory: DatabaseVersionFactory, id: Option[Int] = None,
								number: Option[VersionNumber] = None, created: Option[Instant] = None)
	extends StorableWithFactory[DatabaseVersion]
{
	override def valueProperties = Vector("id" -> id, "version" -> number.map { _.toString }, "created" -> created)
}

