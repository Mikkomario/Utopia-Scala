package utopia.exodus.database.model.user

import java.time.Instant

import utopia.flow.generic.ValueConversions._
import utopia.exodus.database.factory.user.ApiKeyFactory
import utopia.exodus.model.partial.ApiKeyData
import utopia.exodus.model.stored.ApiKey
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object ApiKeyModel
{
	/**
	  * @param key A unique authentication key
	  * @return A model with that key
	  */
	def withKey(key: String) = apply(key = Some(key))
	
	/**
	  * Inserts a new api key to the database
	  * @param data Api key data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted api key
	  */
	def insert(data: ApiKeyData)(implicit connection: Connection) =
	{
		val id = apply(None, Some(data.key), Some(data.creationTime)).insert().getInt
		ApiKey(id, data)
	}
}

/**
  * Used for interacting with api key data in the DB
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
case class ApiKeyModel(id: Option[Int] = None, key: Option[String] = None, created: Option[Instant] = None)
	extends StorableWithFactory[ApiKey]
{
	override def factory = ApiKeyFactory
	
	override def valueProperties = Vector("id" -> id, "key" -> key, "created" -> created)
}
