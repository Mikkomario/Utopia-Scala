package utopia.ambassador.database.model.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthPreparationFactory
import utopia.ambassador.model.partial.process.AuthPreparationData
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.Expiring

/**
  * Used for constructing AuthPreparationModel instances and for inserting AuthPreparations to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthPreparationModel 
	extends DataInserter[AuthPreparationModel, AuthPreparation, AuthPreparationData] with Expiring
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthPreparation userId
	  */
	val userIdAttName = "userId"
	/**
	  * Name of the property that contains AuthPreparation token
	  */
	val tokenAttName = "token"
	/**
	  * Name of the property that contains AuthPreparation expires
	  */
	val expiresAttName = "expires"
	/**
	  * Name of the property that contains client-specified states
	  */
	val clientStateAttName = "clientState"
	/**
	  * Name of the property that contains AuthPreparation created
	  */
	val createdAttName = "created"
	
	override val deprecationAttName = "expires"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthPreparation userId
	  */
	def userIdColumn = table(userIdAttName)
	/**
	  * Column that contains AuthPreparation token
	  */
	def tokenColumn = table(tokenAttName)
	/**
	  * Column that contains AuthPreparation expires
	  */
	def expiresColumn = table(expiresAttName)
	/**
	  * @return Column that contains client-specified states
	  */
	def clientStateColumn = table(clientStateAttName)
	/**
	  * Column that contains AuthPreparation created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthPreparationFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthPreparationData) = 
		apply(None, Some(data.userId), Some(data.token), Some(data.expires), data.clientState, Some(data.created))
	
	override def complete(id: Value, data: AuthPreparationData) = AuthPreparation(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param state A custom state provided by the client and sent back upon user redirect
	  * @return A model with that client state
	  */
	def withClientState(state: String) = apply(clientState = Some(state))
	/**
	  * @param created Time when this AuthPreparation was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param expires Time when this authentication (token) expires
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	/**
	  * @param id A AuthPreparation id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	/**
	  * @param token Token used for authenticating the OAuth redirect
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
	/**
	  * @param userId Id of the user who initiated this process
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with AuthPreparations in the database
  * @param id AuthPreparation database id
  * @param userId Id of the user who initiated this process
  * @param token Token used for authenticating the OAuth redirect
  * @param expires Time when this authentication (token) expires
  * @param clientState A custom state provided by the client and sent back upon user redirect
  * @param created Time when this AuthPreparation was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthPreparationModel(id: Option[Int] = None, userId: Option[Int] = None,
                                token: Option[String] = None, expires: Option[Instant] = None,
                                clientState: Option[String] = None, created: Option[Instant] = None)
	extends StorableWithFactory[AuthPreparation]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationModel.factory
	
	override def valueProperties = 
	{
		import AuthPreparationModel._
		Vector("id" -> id, userIdAttName -> userId, tokenAttName -> token, expiresAttName -> expires, 
			createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
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
	  * @param token A new token
	  * @return A new copy of this model with the specified token
	  */
	def withToken(token: String) = copy(token = Some(token))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}

