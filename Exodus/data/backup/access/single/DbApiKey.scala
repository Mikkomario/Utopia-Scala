package utopia.exodus.database.access.single

import utopia.exodus.database.factory.user.ApiKeyFactory
import utopia.exodus.database.model.user.ApiKeyModel
import utopia.flow.generic.ValueConversions._
import utopia.exodus.model.stored.ApiKey
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccessById

/**
  * Used for accessing individual api keys in the DB
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
object DbApiKey extends SingleModelAccessById[ApiKey, Int]
{
	// IMPLEMENTED	---------------------------------
	
	override def idToValue(id: Int) = id
	
	override def factory = ApiKeyFactory
	
	
	// COMPUTED	-------------------------------------
	
	private def model = ApiKeyModel
	
	
	// OTHER	-------------------------------------
	
	/**
	  * @param key An api key
	  * @param connection DB Connection (implicit)
	  * @return The api key model in DB matching that key. None if not found.
	  */
	def apply(key: String)(implicit connection: Connection) = find(model.withKey(key).toCondition)
	
	/**
	  * @param key An api key
	  * @param connection Database connection (implicit)
	  * @return Whether such a key exists in the database at this time
	  */
	def exists(key: String)(implicit connection: Connection): Boolean = exists(model.withKey(key).toCondition)
}
