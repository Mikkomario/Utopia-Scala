package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidatedSessionFactory
import utopia.exodus.model.partial.auth.EmailValidatedSessionData
import utopia.exodus.model.stored.auth.EmailValidatedSession
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Now
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for constructing EmailValidatedSessionModel instances and for inserting EmailValidatedSessions
  *  to the database
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
@deprecated("Will be removed in a future release", "v4.0")
object EmailValidatedSessionModel 
	extends DataInserter[EmailValidatedSessionModel, EmailValidatedSession, EmailValidatedSessionData] 
		with Deprecatable
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains EmailValidatedSession validationId
	  */
	val validationIdAttName = "validationId"
	
	/**
	  * Name of the property that contains EmailValidatedSession token
	  */
	val tokenAttName = "token"
	
	/**
	  * Name of the property that contains EmailValidatedSession expires
	  */
	val expiresAttName = "expires"
	
	/**
	  * Name of the property that contains EmailValidatedSession created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains EmailValidatedSession closedAfter
	  */
	val closedAfterAttName = "closedAfter"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains EmailValidatedSession validationId
	  */
	def validationIdColumn = table(validationIdAttName)
	
	/**
	  * Column that contains EmailValidatedSession token
	  */
	def tokenColumn = table(tokenAttName)
	
	/**
	  * Column that contains EmailValidatedSession expires
	  */
	def expiresColumn = table(expiresAttName)
	
	/**
	  * Column that contains EmailValidatedSession created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains EmailValidatedSession closedAfter
	  */
	def closedAfterColumn = table(closedAfterAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = EmailValidatedSessionFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def nonDeprecatedCondition = closedAfterColumn.isNull && expiresColumn > Now
	
	override def table = factory.table
	
	override def apply(data: EmailValidatedSessionData) = 
		apply(None, Some(data.validationId), Some(data.token), Some(data.expires), Some(data.created), 
			data.closedAfter)
	
	override def complete(id: Value, data: EmailValidatedSessionData) = EmailValidatedSession(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param closedAfter Time after which this session was manually closed
	  * @return A model containing only the specified closedAfter
	  */
	def withClosedAfter(closedAfter: Instant) = apply(closedAfter = Some(closedAfter))
	
	/**
	  * @param created Time when this EmailValidatedSession was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param expires Time when this EmailValidatedSession expires / becomes invalid
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	
	/**
	  * @param id A EmailValidatedSession id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param token Token used to authenticate against this session
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
	
	/**
	  * @param validationId Reference to the email validation used as the basis for this session
	  * @return A model containing only the specified validationId
	  */
	def withValidationId(validationId: Int) = apply(validationId = Some(validationId))
}

/**
  * Used for interacting with EmailValidatedSessions in the database
  * @param id EmailValidatedSession database id
  * @param validationId Reference to the email validation used as the basis for this session
  * @param token Token used to authenticate against this session
  * @param expires Time when this EmailValidatedSession expires / becomes invalid
  * @param created Time when this EmailValidatedSession was first created
  * @param closedAfter Time after which this session was manually closed
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
@deprecated("Will be removed in a future release", "v4.0")
case class EmailValidatedSessionModel(id: Option[Int] = None, validationId: Option[Int] = None, 
	token: Option[String] = None, expires: Option[Instant] = None, created: Option[Instant] = None, 
	closedAfter: Option[Instant] = None) 
	extends StorableWithFactory[EmailValidatedSession]
{
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidatedSessionModel.factory
	
	override def valueProperties = {
		import EmailValidatedSessionModel._
		Vector("id" -> id, validationIdAttName -> validationId, tokenAttName -> token, 
			expiresAttName -> expires, createdAttName -> created, closedAfterAttName -> closedAfter)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param closedAfter A new closedAfter
	  * @return A new copy of this model with the specified closedAfter
	  */
	def withClosedAfter(closedAfter: Instant) = copy(closedAfter = Some(closedAfter))
	
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
	  * @param validationId A new validationId
	  * @return A new copy of this model with the specified validationId
	  */
	def withValidationId(validationId: Int) = copy(validationId = Some(validationId))
}

