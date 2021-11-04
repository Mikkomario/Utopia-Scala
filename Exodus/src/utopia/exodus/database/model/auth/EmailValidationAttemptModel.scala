package utopia.exodus.database.model.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.Expiring

/**
  * Used for constructing EmailValidationAttemptModel instances and for inserting EmailValidationAttempts to the database
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object EmailValidationAttemptModel 
	extends DataInserter[EmailValidationAttemptModel, EmailValidationAttempt, EmailValidationAttemptData] 
		with Expiring
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains EmailValidationAttempt purposeId
	  */
	val purposeIdAttName = "purposeId"
	/**
	  * Name of the property that contains EmailValidationAttempt email
	  */
	val emailAttName = "email"
	/**
	  * Name of the property that contains EmailValidationAttempt token
	  */
	val tokenAttName = "token"
	/**
	  * Name of the property that contains EmailValidationAttempt resendToken
	  */
	val resendTokenAttName = "resendToken"
	/**
	  * Name of the property that contains EmailValidationAttempt userId
	  */
	val userIdAttName = "userId"
	/**
	  * Name of the property that contains EmailValidationAttempt expires
	  */
	val expiresAttName = "expires"
	/**
	  * Name of the property that contains EmailValidationAttempt created
	  */
	val createdAttName = "created"
	/**
	  * Name of the property that contains EmailValidationAttempt completed
	  */
	val completedAttName = "completed"
	
	override val deprecationAttName = "expires"
	
	/**
	  * Condition that only accepts email validation attempts that haven't been completed yet
	  */
	lazy val incompleteCondition = completedColumn.isNull
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains EmailValidationAttempt purposeId
	  */
	def purposeIdColumn = table(purposeIdAttName)
	/**
	  * Column that contains EmailValidationAttempt email
	  */
	def emailColumn = table(emailAttName)
	/**
	  * Column that contains EmailValidationAttempt token
	  */
	def tokenColumn = table(tokenAttName)
	/**
	  * Column that contains EmailValidationAttempt resendToken
	  */
	def resendTokenColumn = table(resendTokenAttName)
	/**
	  * Column that contains EmailValidationAttempt userId
	  */
	def userIdColumn = table(userIdAttName)
	/**
	  * Column that contains EmailValidationAttempt expires
	  */
	def expiresColumn = table(expiresAttName)
	/**
	  * Column that contains EmailValidationAttempt created
	  */
	def createdColumn = table(createdAttName)
	/**
	  * Column that contains EmailValidationAttempt completed
	  */
	def completedColumn = table(completedAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = EmailValidationAttemptFactory
	
	/**
	  * @return A model that has just been marked as completed
	  */
	def nowCompleted = withCompleted(Now)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: EmailValidationAttemptData) = 
		apply(None, Some(data.purposeId), Some(data.email), Some(data.token), Some(data.resendToken), 
			Some(data.expires), data.userId, Some(data.created), data.completed)
	
	override def complete(id: Value, data: EmailValidationAttemptData) = EmailValidationAttempt(id.getInt, 
		data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param completed Time when this attempt was finished successfully. None while not completed.
	  * @return A model containing only the specified completed
	  */
	def withCompleted(completed: Instant) = apply(completed = Some(completed))
	/**
	  * @param created Time when this EmailValidationAttempt was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param email Email address being validated
	  * @return A model containing only the specified email
	  */
	def withEmail(email: String) = apply(email = Some(email))
	/**
	  * @param expires Time when this EmailValidationAttempt expires / becomes invalid
	  * @return A model containing only the specified expires
	  */
	def withExpires(expires: Instant) = apply(expires = Some(expires))
	/**
	  * @param id A EmailValidationAttempt id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	/**
	  * @param purposeId Id of the purpose this email validation is used for
	  * @return A model containing only the specified purposeId
	  */
	def withPurposeId(purposeId: Int) = apply(purposeId = Some(purposeId))
	/**
	  * @param resendToken Token used for authenticating an email resend attempt
	  * @return A model containing only the specified resendToken
	  */
	def withResendToken(resendToken: String) = apply(resendToken = Some(resendToken))
	/**
	  * @param token Token sent with the email, which is also used for validating the email address
	  * @return A model containing only the specified token
	  */
	def withToken(token: String) = apply(token = Some(token))
	/**
	  * @param userId Id of the user who claims to own this email address
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with EmailValidationAttempts in the database
  * @param id EmailValidationAttempt database id
  * @param purposeId Id of the purpose this email validation is used for
  * @param email Email address being validated
  * @param token Token sent with the email, which is also used for validating the email address
  * @param resendToken Token used for authenticating an email resend attempt
  * @param expires Time when this EmailValidationAttempt expires / becomes invalid
  * @param userId Id of the user who claims to own this email address
  * @param created Time when this EmailValidationAttempt was first created
  * @param completed Time when this attempt was finished successfully. None while not completed.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class EmailValidationAttemptModel(id: Option[Int] = None, purposeId: Option[Int] = None, 
	email: Option[String] = None, token: Option[String] = None, resendToken: Option[String] = None, 
	expires: Option[Instant] = None, userId: Option[Int] = None, created: Option[Instant] = None,
	completed: Option[Instant] = None) 
	extends StorableWithFactory[EmailValidationAttempt]
{
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptModel.factory
	
	override def valueProperties = 
	{
		import EmailValidationAttemptModel._
		Vector("id" -> id, purposeIdAttName -> purposeId, emailAttName -> email, tokenAttName -> token, 
			resendTokenAttName -> resendToken, userIdAttName -> userId, expiresAttName -> expires, 
			createdAttName -> created, completedAttName -> completed)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param completed A new completed
	  * @return A new copy of this model with the specified completed
	  */
	def withCompleted(completed: Instant) = copy(completed = Some(completed))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param email A new email
	  * @return A new copy of this model with the specified email
	  */
	def withEmail(email: String) = copy(email = Some(email))
	
	/**
	  * @param expires A new expires
	  * @return A new copy of this model with the specified expires
	  */
	def withExpires(expires: Instant) = copy(expires = Some(expires))
	
	/**
	  * @param purposeId A new purposeId
	  * @return A new copy of this model with the specified purposeId
	  */
	def withPurposeId(purposeId: Int) = copy(purposeId = Some(purposeId))
	
	/**
	  * @param resendToken A new resendToken
	  * @return A new copy of this model with the specified resendToken
	  */
	def withResendToken(resendToken: String) = copy(resendToken = Some(resendToken))
	
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

