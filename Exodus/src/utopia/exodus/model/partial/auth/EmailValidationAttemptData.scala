package utopia.exodus.model.partial.auth

import java.time.Instant
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

/**
  * Represents an attempted email validation, and the possible response / success
  * @param purposeId Id of the purpose this email validation is used for
  * @param email Email address being validated
  * @param token Token sent with the email, which is also used for validating the email address
  * @param resendToken Token used for authenticating an email resend attempt
  * @param userId Id of the user who claims to own this email address (if applicable)
  * @param expires Time when this EmailValidationAttempt expires / becomes invalid
  * @param created Time when this EmailValidationAttempt was first created
  * @param completed Time when this attempt was finished successfully. None while not completed.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class EmailValidationAttemptData(purposeId: Int, email: String, token: String, resendToken: String, 
	expires: Instant, userId: Option[Int] = None, created: Instant = Now, completed: Option[Instant] = None)
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this EmailValidationAttempt is no longer valid because it has expired
	  */
	def hasExpired = expires <= Now
	/**
	  * Whether this EmailValidationAttempt is still valid (hasn't expired yet)
	  */
	def isValid = !hasExpired
	
	/**
	  * @return Whether this validation was actually answered
	  */
	def wasCompleted = completed.nonEmpty
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("purpose_id" -> purposeId, "email" -> email, "token" -> token, 
			"resend_token" -> resendToken, "user_id" -> userId, "expires" -> expires, "created" -> created, 
			"completed" -> completed))
}

