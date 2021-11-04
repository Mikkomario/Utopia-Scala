package utopia.ambassador.database.model.process

import java.time.Instant
import utopia.ambassador.database.factory.process.IncompleteAuthFactory
import utopia.ambassador.model.partial.process.IncompleteAuthData
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.Expiring

/**
  * Used for constructing IncompleteAuthModel instances and for inserting IncompleteAuths to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object IncompleteAuthModel 
	extends DataInserter[IncompleteAuthModel, IncompleteAuth, IncompleteAuthData] with Expiring
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains IncompleteAuth serviceId
	  */
	val serviceIdAttName = "serviceId"
	
	/**
	  * Name of the property that contains IncompleteAuth code
	  */
	val codeAttName = "code"
	
	/**
	  * Name of the property that contains IncompleteAuth token
	  */
	val tokenAttName = "token"
	
	/**
	  * Name of the property that contains IncompleteAuth expires
	  */
	val expiresAttName = "expires"
	
	/**
	  * Name of the property that contains IncompleteAuth created
	  */
	val createdAttName = "created"
	
	override val deprecationAttName = "expires"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains IncompleteAuth serviceId
	  */
	def serviceIdColumn = table(serviceIdAttName)
	
	/**
	  * Column that contains IncompleteAuth code
	  */
	def codeColumn = table(codeAttName)
	
	/**
	  * Column that contains IncompleteAuth token
	  */
	def tokenColumn = table(tokenAttName)
	
	/**
	  * Column that contains IncompleteAuth expires
	  */
	def expiresColumn = table(expiresAttName)
	
	/**
	  * Column that contains IncompleteAuth created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = IncompleteAuthFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: IncompleteAuthData) = 
		apply(None, Some(data.serviceId), Some(data.code), Some(data.token), Some(data.expires), 
			Some(data.created))
	
	override def complete(id: Value, data: IncompleteAuthData) = IncompleteAuth(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param code Authentication code provided by the 3rd party service
	  * @return A model containing only the specified code
	  */
	def withCode(code: String) = apply(code = Some(code))
	
	/**
	  * @param created Time when this IncompleteAuth was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param expires Time after which the generated authentication token is no longer valid
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	
	/**
	  * @param id A IncompleteAuth id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param serviceId Id of the service from which the user arrived
	  * @return A model containing only the specified serviceId
	  */
	def withServiceId(serviceId: Int) = apply(serviceId = Some(serviceId))
	
	/**
	  * @param token Token used for authentication the completion of this authentication
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
}

/**
  * Used for interacting with IncompleteAuths in the database
  * @param id IncompleteAuth database id
  * @param serviceId Id of the service from which the user arrived
  * @param code Authentication code provided by the 3rd party service
  * @param token Token used for authentication the completion of this authentication
  * @param expires Time after which the generated authentication token is no longer valid
  * @param created Time when this IncompleteAuth was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class IncompleteAuthModel(id: Option[Int] = None, serviceId: Option[Int] = None, 
	code: Option[String] = None, token: Option[String] = None, expires: Option[Instant] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[IncompleteAuth]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthModel.factory
	
	override def valueProperties = 
	{
		import IncompleteAuthModel._
		Vector("id" -> id, serviceIdAttName -> serviceId, codeAttName -> code, tokenAttName -> token, 
			expiresAttName -> expires, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param code A new code
	  * @return A new copy of this model with the specified code
	  */
	def withCode(code: String) = copy(code = Some(code))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param expires A new expires
	  * @return A new copy of this model with the specified expires
	  */
	def withExpires(expires: Instant) = copy(expires = Some(expires))
	
	/**
	  * @param serviceId A new serviceId
	  * @return A new copy of this model with the specified serviceId
	  */
	def withServiceId(serviceId: Int) = copy(serviceId = Some(serviceId))
	
	/**
	  * @param token A new token
	  * @return A new copy of this model with the specified token
	  */
	def withToken(token: String) = copy(token = Some(token))
}

