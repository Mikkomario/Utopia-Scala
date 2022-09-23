package utopia.ambassador.database.model.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthRedirectFactory
import utopia.ambassador.model.partial.process.AuthRedirectData
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.Expiring

/**
  * Used for constructing AuthRedirectModel instances and for inserting AuthRedirects to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthRedirectModel extends DataInserter[AuthRedirectModel, AuthRedirect, AuthRedirectData] with Expiring
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains AuthRedirect preparationId
	  */
	val preparationIdAttName = "preparationId"
	
	/**
	  * Name of the property that contains AuthRedirect token
	  */
	val tokenAttName = "token"
	
	/**
	  * Name of the property that contains AuthRedirect expires
	  */
	val expiresAttName = "expires"
	
	/**
	  * Name of the property that contains AuthRedirect created
	  */
	val createdAttName = "created"
	
	override val deprecationAttName = "expires"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains AuthRedirect preparationId
	  */
	def preparationIdColumn = table(preparationIdAttName)
	
	/**
	  * Column that contains AuthRedirect token
	  */
	def tokenColumn = table(tokenAttName)
	
	/**
	  * Column that contains AuthRedirect expires
	  */
	def expiresColumn = table(expiresAttName)
	
	/**
	  * Column that contains AuthRedirect created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = AuthRedirectFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: AuthRedirectData) = 
		apply(None, Some(data.preparationId), Some(data.token), Some(data.expires), Some(data.created))
	
	override def complete(id: Value, data: AuthRedirectData) = AuthRedirect(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this AuthRedirect was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param expires Time when the linked redirect token expires
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	
	/**
	  * @param id A AuthRedirect id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param preparationId Id of the preparation event for this redirection
	  * @return A model containing only the specified preparationId
	  */
	def withPreparationId(preparationId: Int) = apply(preparationId = Some(preparationId))
	
	/**
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
}

/**
  * Used for interacting with AuthRedirects in the database
  * @param id AuthRedirect database id
  * @param preparationId Id of the preparation event for this redirection
  * @param expires Time when the linked redirect token expires
  * @param created Time when this AuthRedirect was first created
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class AuthRedirectModel(id: Option[Int] = None, preparationId: Option[Int] = None, 
	token: Option[String] = None, expires: Option[Instant] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[AuthRedirect]
{
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectModel.factory
	
	override def valueProperties = 
	{
		import AuthRedirectModel._
		Vector("id" -> id, preparationIdAttName -> preparationId, tokenAttName -> token, 
			expiresAttName -> expires, createdAttName -> created)
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
	  * @param preparationId A new preparationId
	  * @return A new copy of this model with the specified preparationId
	  */
	def withPreparationId(preparationId: Int) = copy(preparationId = Some(preparationId))
	
	/**
	  * @param token A new token
	  * @return A new copy of this model with the specified token
	  */
	def withToken(token: String) = copy(token = Some(token))
}

