package utopia.exodus.database.access.single.auth

import scala.concurrent.duration.Duration
import utopia.citadel.database.access.single.user.DbUserSettings
import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.enumeration.ExodusTokenType.EmailValidationToken
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.exodus.util.UuidGenerator
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

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
	  * Inserts a new email validation attempt to the database
	  * @param emailAddress Email address of the intended recipient
	  * @param tokenScopeIds Ids of the scopes accessible using the generated email validation token (default = empty)
	  * @param purposeId Id of the purpose this email validation attempt is for
	  * @param forwardedScopeIds Ids of the scopes accessible using an email validated session based on the
	  * specified token (default = empty)
	  * @param parentTokenId Id of the token authorized to make this attempt (default = None)
	  * @param modelStyle Model style to tie with the resulting token (default = None)
	  * @param customDuration Life duration to assign to the generated token, if different from token type default
	  * (default = None)
	  * @param tokenTypeId Id of the type of the generated token (default = email validation token)
	  * @param limitToDefaultDuration Whether specified custom duration should be restricted to token type default
	  * duration (default = false)
	  * @param connection Implicit DB Connection
	  * @param uuidGenerator Implicit UUID generator
	  * @return Inserted email validation attempt + inserted token + email validation token string
	  */
	def insert(emailAddress: String, purposeId: Int, tokenScopeIds: Set[Int] = Set(),
	           forwardedScopeIds: Set[Int] = Set(), parentTokenId: Option[Int] = None,
	           modelStyle: Option[ModelStyle] = None, customDuration: Option[Duration] = None,
	           tokenTypeId: Int = EmailValidationToken.id, limitToDefaultDuration: Boolean = false)
	          (implicit connection: Connection, uuidGenerator: UuidGenerator) =
	{
		// Inserts a new token
		val (token, tokenString) = DbToken.insert(tokenTypeId, parentTokenId,
			DbUserSettings.withEmail(emailAddress).userId, tokenScopeIds, forwardedScopeIds, modelStyle,
			customDuration, limitToDefaultDuration)
		// Inserts a new email validation attempt, also
		val attempt = model.insert(EmailValidationAttemptData(token.id, emailAddress, purposeId))
		
		(attempt, token, tokenString)
	}
	
	/**
	  * @param tokenId Id of an access token
	  * @return An access point to an email validation attempt where that token was sent
	  */
	def usingTokenWithId(tokenId: Int) = new DbEmailValidationAttemptWithToken(tokenId)
	
	
	// NESTED	--------------------
	
	class DbEmailValidationAttemptWithToken(tokenId: Int) extends UniqueEmailValidationAttemptAccess 
		with SubView
	{
		// IMPLEMENTED	--------------------
		
		override def filterCondition = model.withTokenId(tokenId).toCondition
		
		override protected def parent = DbEmailValidationAttempt
	}
}

