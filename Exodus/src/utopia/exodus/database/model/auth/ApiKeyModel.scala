package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.ApiKeyFactory
import utopia.exodus.model.partial.auth.ApiKeyData
import utopia.exodus.model.stored.auth.ApiKey
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ApiKeyModel instances and for inserting ApiKeys to the database
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object ApiKeyModel extends DataInserter[ApiKeyModel, ApiKey, ApiKeyData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains ApiKey token
	  */
	val tokenAttName = "token"
	
	/**
	  * Name of the property that contains ApiKey name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains ApiKey created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains ApiKey token
	  */
	def tokenColumn = table(tokenAttName)
	
	/**
	  * Column that contains ApiKey name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains ApiKey created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ApiKeyFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ApiKeyData) = apply(None, Some(data.token), Some(data.name), Some(data.created))
	
	override def complete(id: Value, data: ApiKeyData) = ApiKey(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this ApiKey was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A ApiKey id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param name Name given to identify this api key
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
	
	/**
	  * @param token The textual representation of this api key
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
}

/**
  * Used for interacting with ApiKeys in the database
  * @param id ApiKey database id
  * @param token The textual representation of this api key
  * @param name Name given to identify this api key
  * @param created Time when this ApiKey was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class ApiKeyModel(id: Option[Int] = None, token: Option[String] = None, name: Option[String] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[ApiKey]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ApiKeyModel.factory
	
	override def valueProperties = 
	{
		import ApiKeyModel._
		Vector("id" -> id, tokenAttName -> token, nameAttName -> name, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param name A new name
	  * @return A new copy of this model with the specified name
	  */
	def withName(name: String) = copy(name = Some(name))
	
	/**
	  * @param token A new token
	  * @return A new copy of this model with the specified token
	  */
	def withToken(token: String) = copy(token = Some(token))
}

