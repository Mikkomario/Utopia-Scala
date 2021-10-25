package utopia.exodus.database.access.single.auth

import utopia.access.http.Status.Forbidden
import utopia.exodus.database.factory.auth.EmailValidationAttemptFactory
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.error.InvalidKeyException
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.exodus.util.EmailValidator
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

import scala.util.{Failure, Success}

/**
  * Used for accessing individual EmailValidationAttempts
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbEmailValidationAttempt 
	extends SingleRowModelAccess[EmailValidationAttempt] with NonDeprecatedView[EmailValidationAttempt] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidationAttemptModel
	
	/**
	  * @return An access point to individual email validation attempts that are currently open (incomplete)
	  */
	def open = DbOpenEmailValidationAttempt
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidationAttemptFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted EmailValidationAttempt instance
	  * @return An access point to that EmailValidationAttempt
	  */
	def apply(id: Int) = DbSingleEmailValidationAttempt(id)
	
	/**
	  * Starts a new email validation attempt
	  * @param emailAddress Targeted email address
	  * @param purposeId Id representing the purpose of this validation
	  * @param ownerId Id of the user who claims to own this email (optional)
	  * @param connection DB Connection (implicit)
	  * @param validator An email validator implementation to use (implicit)
	  * @return Either<br>
	  *         Right) Recorded email validation attempt or<br>
	  *         Left) Recommended response status and a descriptive message
	  */
	// TODO: Apply a limit based on origin ip address once that feature is available
	def start(emailAddress: String, purposeId: Int, ownerId: Option[Int] = None)
	         (implicit connection: Connection, validator: EmailValidator) =
	{
		// Checks whether there already exists an open validation attempt
		open.find(emailAddress, purposeId) match
		{
			// Case: There already exists an open email validation attempt => attempts to send it again (may be limited)
			case Some(existing) =>
				apply(existing.id).tryRecordResend(validator.maximumResendsPerValidation) match
				{
					case Success(_) =>
						validator.resend(existing)
						Right(existing)
					case Failure(error) => Left(Forbidden -> error.getMessage)
				}
			// Case: No attempt exists yet => starts a new one
			case None => validator.validate(emailAddress, purposeId, ownerId).map { model.insert(_) }
		}
	}
	
	
	// NESTED   --------------------
	
	object DbOpenEmailValidationAttempt extends SingleRowModelAccess[EmailValidationAttempt] with SubView
	{
		// IMPLEMENTED  ------------
		
		override protected def parent = DbEmailValidationAttempt
		override def factory = parent.factory
		
		override def filterCondition = model.incompleteCondition
		
		
		// OTHER    ---------------
		
		/**
		  * @param resendToken A resend token
		  * @return An access point to an open email validation attempt matching that resend token
		  */
		def withResendToken(resendToken: String) = new DbResendableEmailValidation(resendToken)
		
		/**
		  * Finds an existing non-deprecated email validation record
		  * @param email Targeted email address
		  * @param purposeId Id of the purpose of this validation
		  * @param connection DB connection (implicit)
		  * @return Existing email validation attempt that is still active / open. None if there are no active / open
		  *         validation attempts at this time.
		  */
		def find(email: String, purposeId: Int)(implicit connection: Connection): Option[EmailValidationAttempt] =
			find(model.withEmail(email).withPurposeId(purposeId).toCondition)
		
		/**
		  * Finds an active / open, non-actualized, non-deprecated email validation that corresponds with the specified
		  * validation token and purpose. Performs the specified operation using this validation's data and possibly closes
		  * it afterwards.
		  * @param token An email validation token (usually send with the email)
		  * @param purposeId Id of the purpose for which this token was created / for which it is being used
		  *                  (these must match)
		  * @param f A function called if there existed a valid matching token for the specified purpose.
		  *          The function accepts email validation data and returns
		  *          1) a boolean indicating whether the validation should be closed and
		  *          2) additional final result
		  * @param connection DB Connection (implicit)
		  * @return The final result of the specified function on success. Failure if the specified token couldn't be
		  *         validated for the specified purpose (token was invalid, expired, for a different purpose or
		  *         already closed).
		  */
		def completeWithToken[A](token: String, purposeId: Int)(f: EmailValidationAttempt => (Boolean, A))
		                      (implicit connection: Connection) =
			find(model.withToken(token).withPurposeId(purposeId).toCondition) match
			{
				// Case: Specified key is valid
				case Some(openValidation) =>
					// Performs the requested operation
					val (closeValidation, result) = f(openValidation)
					// May close / actualize this email validation
					if (closeValidation)
						openValidation.access.complete()
					Success(result)
				// Case: Specified key is invalid
				case None => Failure(new InvalidKeyException(
					"Specified email activation key is either invalid, used or expired"))
			}
		
		// NESTED   -----------------------------
		
		class DbResendableEmailValidation(resendToken: String) extends UniqueEmailValidationAttemptAccess with SubView
		{
			override protected def parent = DbOpenEmailValidationAttempt
			override protected def defaultOrdering = None
			
			override def filterCondition = model.withResendToken(resendToken).toCondition
		}
	}
}

