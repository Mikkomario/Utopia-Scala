package utopia.exodus.database.access.single.auth

import utopia.access.http.Status
import utopia.access.http.Status.{Forbidden, Unauthorized}

import java.time.Instant
import utopia.exodus.database.factory.auth.EmailValidationAttemptFactoryOld
import utopia.exodus.database.model.auth.EmailValidationAttemptModelOld
import utopia.exodus.model.stored.auth.EmailValidationAttemptOld
import utopia.exodus.util.EmailValidator
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

import scala.util.{Failure, Success}

/**
  * A common trait for access points that return individual and distinct EmailValidationAttempts.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Replaced with a new version", "v4.0")
trait UniqueEmailValidationAttemptAccessOld
	extends SingleRowModelAccess[EmailValidationAttemptOld]
		with DistinctModelAccess[EmailValidationAttemptOld, Option[EmailValidationAttemptOld], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the purpose this email validation is used for. None if no instance (or value) was found.
	  */
	def purposeId(implicit connection: Connection) = pullColumn(model.purposeIdColumn).int
	/**
	  * Email address being validated. None if no instance (or value) was found.
	  */
	def email(implicit connection: Connection) = pullColumn(model.emailColumn).string
	/**
	  * Token sent 
		with the email, which is also used for validating the email address. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	/**
	  * Token used for authenticating an email resend attempt. None if no instance (or value) was found.
	  */
	def resendToken(implicit connection: Connection) = pullColumn(model.resendTokenColumn).string
	/**
	  * Id of the user who claims to own this email address. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	/**
	  * Time when this EmailValidationAttempt expires / becomes invalid. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	/**
	  * Time when this EmailValidationAttempt was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	/**
	  * Time when this attempt was finished successfully. None while not completed.. None if no instance (or value) was found.
	  */
	def completed(implicit connection: Connection) = pullColumn(model.completedColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModelOld
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactoryOld
	
	
	// OTHER	--------------------
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Whether this attempt was affected (wasn't already completed)
	  */
	def complete()(implicit connection: Connection) = completed = Now
	
	/**
	  * Attempts to send this email validation again. Fails if too many resends have been attempted already.
	  * NB: Doesn't check whether this email validation attempt should be resent (i.e. is not closed etc.)
	  * @param connection DB Connection (implicit)
	  * @param validator An email validator that will perform the actual resend operation (implicit)
	  * @return Either<br>
	  *         Right) Unit on success or<br>
	  *         Left) Proposed error status code and a description / message (failure state)
	  */
	def resend()(implicit connection: Connection, validator: EmailValidator): Either[(Status, String), Unit] =
		pull match
		{
			case Some(validation) =>
				// Records the resend (attempt) and checks whether maximum number of resends was exceeded
				validation.access.tryRecordResend(validator.maximumResendsPerValidation) match
				{
					case Success(_) => Right(validator.resend(validation))
					case Failure(error) => Left(Forbidden -> error.getMessage)
				}
			case None => Left(Unauthorized -> "Specified resend key is invalid or expired")
		}
	
	/**
	  * Updates the completed of the targeted EmailValidationAttempt instance(s)
	  * @param newCompleted A new completed to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def completed_=(newCompleted: Instant)(implicit connection: Connection) = 
		putColumn(model.completedColumn, newCompleted)
	/**
	  * Updates the created of the targeted EmailValidationAttempt instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the email of the targeted EmailValidationAttempt instance(s)
	  * @param newEmail A new email to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def email_=(newEmail: String)(implicit connection: Connection) = putColumn(model.emailColumn, newEmail)
	/**
	  * Updates the expires of the targeted EmailValidationAttempt instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	/**
	  * Updates the purposeId of the targeted EmailValidationAttempt instance(s)
	  * @param newPurposeId A new purposeId to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def purposeId_=(newPurposeId: Int)(implicit connection: Connection) = 
		putColumn(model.purposeIdColumn, newPurposeId)
	/**
	  * Updates the resendToken of the targeted EmailValidationAttempt instance(s)
	  * @param newResendToken A new resendToken to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def resendToken_=(newResendToken: String)(implicit connection: Connection) = 
		putColumn(model.resendTokenColumn, newResendToken)
	/**
	  * Updates the token of the targeted EmailValidationAttempt instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	/**
	  * Updates the userId of the targeted EmailValidationAttempt instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any EmailValidationAttempt instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

