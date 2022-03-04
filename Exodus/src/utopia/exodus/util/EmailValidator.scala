package utopia.exodus.util

import utopia.exodus.database.access.single.auth.DbEmailValidationAttempt
import utopia.exodus.model.combined.auth.DetailedToken
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.metropolis.model.enumeration.ModelStyle
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.database.Connection

import scala.concurrent.duration.Duration
import scala.util.Try

/**
  * A common trait for email validation logic implementations. Usually these validators send an email to the
  * specified address with some means of connecting the contents of that email with the service
  * @author Mikko Hilpinen
  * @since 1.12.2020, v1
  */
trait EmailValidator
{
	// ABSTRACT -------------------------------
	
	/**
	  * @param purposeId Id of the purpose the linked email validation is for
	  * @return Id of the token type to apply to that validation's token
	  */
	def tokenTypeIdForPurposeWithId(purposeId: Int): Int
	
	/**
	  * Possibly modifies the scopes proposed for an email validation token
	  * @param purposeId Id of the purpose of the linked email validation
	  * @param proposedAccessScopeIds Proposed scope ids given to the token directly
	  * @param proposedForwardedScopeIds Proposed scope ids to give to a secondary access token
	  *                                  (acquired through using the email validation token as a refresh token)
	  * @param connection Implicit DB Connection
	  * @return Applied access scope ids + applied forwarded scope ids
	  */
	def customizeScopes(purposeId: Int, proposedAccessScopeIds: Set[Int], proposedForwardedScopeIds: Set[Int])
	                   (implicit connection: Connection): (Set[Int], Set[Int])
	
	/**
	  * Possibly specifies a custom duration for an email validation token
	  * @param purposeId Id of the purpose the linked email validation is made for
	  * @param tokenTypeId Id of the applied token type
	  * @return None if token type default duration should be used. Some(Duration, Boolean) if a custom duration
	  *         should be used, where the first value specifies a custom duration and the second value specifies
	  *         whether the custom value should be limited to token type default (as maximum).
	  */
	def customizeTokenDuration(purposeId: Int, tokenTypeId: Int): Option[(Duration, Boolean)]
	
	/**
	  * Makes a new email validation attempt by sending an email to the targeted address
	  * @param attempt Email validation attempt information
	  * @param token Generated access token
	  * @param tokenString A non-hashed token to send to the recipient
	  * @param invitation A possible linked invitation, if applicable
	  * @param connection Implicit DB Connection
	  * @return Success or failure
	  */
	def send(attempt: EmailValidationAttempt, token: DetailedToken, tokenString: String,
	         invitation: Option[Invitation] = None)(implicit connection: Connection): Try[Unit]
	
	/**
	  * Sends an email to the specified email address instead of sending an email validation token. This method is
	  * called in situations where the recipient shouldn't be authorized for further action, but should simply be
	  * informed about the failure of the linked action. For example, when attempting password recovery with an
	  * email address that is not registered in the system, should inform the owner of that email address.
	  * @param emailAddress Email address to which to send the message
	  * @param purposeId Id of the original purpose of the email validation attempt
	  * @param message A short description of the situation
	  * @param connection Implicit DB Connection
	  * @return A success or a failure
	  */
	def sendWithoutToken(emailAddress: String, purposeId: Int, message: String)
	                    (implicit connection: Connection): Try[Unit]
	
	
	// OTHER    ---------------------------
	
	/**
	  * Performs an email validation attempt
	  * @param emailAddress Targeted email address (should be validated at this point)
	  * @param purposeId Id of the purpose this validation attempt is for
	  * @param proposedAccessScopeIds Proposed access scope ids to give to the generated email validation token
	  *                               (default = empty)
	  * @param proposedForwardedScopeIds Proposed scope ids to give to token(s) acquired through using the generated
	  *                                  email validation token as a refresh token (default = empty)
	  * @param parentTokenId Id of the token used to authorize this attempt, if known (default = empty)
	  * @param modelStylePreference A model style preference to apply to requests made with the generated token(s)
	  *                             (optional)
	  * @param invitation A possible linked invitation, if applicable
	  * @param connection Implicit DB Connection
	  * @param uuidGenerator Implicit UUID generator
	  * @return Success or failure
	  */
	def apply(emailAddress: String, purposeId: Int, proposedAccessScopeIds: Set[Int] = Set(),
	          proposedForwardedScopeIds: Set[Int] = Set(), parentTokenId: Option[Int] = None,
	          modelStylePreference: Option[ModelStyle] = None, invitation: Option[Invitation] = None)
	         (implicit connection: Connection, uuidGenerator: UuidGenerator) =
	{
		// Customizes & specifies proposed values
		val tokenTypeId = tokenTypeIdForPurposeWithId(purposeId)
		val (accessScopeIds, forwardedScopeIds) = customizeScopes(purposeId, proposedAccessScopeIds,
			proposedForwardedScopeIds)
		val durationMod = customizeTokenDuration(purposeId, tokenTypeId)
		
		// Inserts a new email validation attempt to the DB
		val (attempt, token, tokenString) = DbEmailValidationAttempt.insert(emailAddress, purposeId, accessScopeIds,
			forwardedScopeIds, parentTokenId, modelStylePreference, durationMod.map { _._1 }, tokenTypeId,
			durationMod.exists { _._2 })
		
		// Sends the email
		send(attempt, token, tokenString, invitation)
	}
}
