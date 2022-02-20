package utopia.exodus.database.access.single.auth

import utopia.citadel.database.access.single.user.DbUserSettings
import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.enumeration.ExodusTokenType
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.exodus.util.UuidGenerator
import utopia.flow.util.Sha256Hasher
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

import scala.concurrent.duration.Duration

/**
  * Used for accessing individual email validation attempts
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object DbEmailValidationAttempt 
	extends SingleRowModelAccess[EmailValidationAttempt] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted email validation attempt
	  * @return An access point to that email validation attempt
	  */
	def apply(id: Int) = DbSingleEmailValidationAttempt(id)
	
	/**
	  * @param tokenId Id of an access token
	  * @return An access point to an email validation attempt where that token was sent
	  */
	def usingTokenWithId(tokenId: Int) = new DbEmailValidationAttemptWithToken(tokenId)
	
	/**
	  * Inserts a new email validation attempt to the database
	  * @param emailAddress Email address of the intended recipient
	  * @param tokenScopeIds Ids of the scopes accessible using the generated email validation token (default = empty)
	  * @param forwardedScopeIds Ids of the scopes accessible using an email validated session based on the
	  *                          specified token (default = empty)
	  * @param parentTokenId Id of the token authorized to make this attempt (default = None)
	  * @param previousSendAttempts Number of previous email validation attempts in this context (default = 0)
	  * @param modelStyle Model style to tie with the resulting token (default = None)
	  * @param customDuration Life duration to assign to the generated token, if different from token type default
	  *                       (default = None)
	  * @param tokenTypeId Id of the type of the generated token (default = email validation token)
	  * @param limitToDefaultDuration Whether specified custom duration should be restricted to token type default
	  *                               duration (default = false)
	  * @param disableResend Whether resend feature should be disabled (default = false)
	  * @param connection Implicit DB Connection
	  * @param uuidGenerator Implicit UUID generator
	  * @return Inserted email validation attempt + email validation token string + resend token string
	  */
	def insert(emailAddress: String, tokenScopeIds: Set[Int] = Set(), forwardedScopeIds: Set[Int] = Set(),
	          parentTokenId: Option[Int] = None, previousSendAttempts: Int = 0, modelStyle: Option[ModelStyle] = None,
	          customDuration: Option[Duration] = None, tokenTypeId: Int = ExodusTokenType.EmailValidationToken.id,
	          limitToDefaultDuration: Boolean = false, disableResend: Boolean = false)
	         (implicit connection: Connection, uuidGenerator: UuidGenerator) =
	{
		// Inserts a new token
		val (token, tokenString) = DbToken.insert(tokenTypeId, parentTokenId,
			DbUserSettings.withEmail(emailAddress).userId, tokenScopeIds, forwardedScopeIds, modelStyle,
			customDuration, limitToDefaultDuration)
		val resendToken = {
			if (disableResend)
				None
			else
				Some(uuidGenerator.next())
		}
		// Inserts a new email validation attempt, also
		val attempt = model.insert(EmailValidationAttemptData(token.id, emailAddress,
			resendToken.map { Sha256Hasher(_) }, previousSendAttempts + 1))
		
		(attempt, tokenString, resendToken)
	}
	
	
	// NESTED	--------------------
	
	class DbEmailValidationAttemptWithToken(tokenId: Int) extends UniqueEmailValidationAttemptAccess 
		with SubView
	{
		// IMPLEMENTED	--------------------
		
		override def filterCondition = model.withTokenId(tokenId).toCondition
		
		override protected def parent = DbEmailValidationAttempt
	}
}

