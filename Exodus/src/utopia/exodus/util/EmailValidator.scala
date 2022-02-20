package utopia.exodus.util

import utopia.access.http.Status
import utopia.exodus.model.partial.auth.EmailValidationAttemptDataOld
import utopia.exodus.model.stored.auth.EmailValidationAttemptOld
import utopia.vault.database.Connection

/**
  * A common trait for email validation logic implementations. Usually these validators send an email to the
  * specified address with some means of connecting the contents of that email with the service
  * @author Mikko Hilpinen
  * @since 1.12.2020, v1
  */
// TODO: Rework this trait to support the new DB structure & logic
trait EmailValidator
{
	/**
	  * @return The maximum number of allowed validation resends
	  */
	def maximumResendsPerValidation: Int
	
	/**
	  * Attempts to validate the specified email address
	  * @param email Targeted email address
	  * @param purposeId Id of the purpose of this validation attempt
	  * @param ownerId Id of the user who owns or wants to own the email address, if known (default = None)
	  * @param connection Implicit database connection
	  * @return Either:<br>
	  *         Right) Email validation data to insert to the database or<br>
	  *         Left) Response (failure) status and message / description
	  */
	def validate(email: String, purposeId: Int, ownerId: Option[Int] = None)
				(implicit connection: Connection): Either[(Status, String), EmailValidationAttemptDataOld]
	
	/**
	  * Reattempts the validation procedure
	  * @param previous Previously attempted email validation
	  * @param connection DB Connection (implicit)
	  */
	def resend(previous: EmailValidationAttemptOld)(implicit connection: Connection): Unit
}
