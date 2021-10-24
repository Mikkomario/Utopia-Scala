package utopia.exodus.database.access.single

import utopia.access.http.Status
import utopia.access.http.Status.{Forbidden, Unauthorized}
import utopia.exodus.database.access.id.DbEmailValidationId
import utopia.exodus.database.factory.user.EmailValidationFactory
import utopia.exodus.database.model.user.EmailValidationResendModel
import utopia.exodus.model.error.{InvalidKeyException, TooManyRequestsException}
import utopia.exodus.model.stored.EmailValidation
import utopia.exodus.util.EmailValidator
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.vault.sql.{Count, Where}

import scala.util.{Failure, Success}

/**
  * Used for interacting with individual email validations in the DB
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
object DbEmailValidation extends SingleModelAccess[EmailValidation]
{
	// IMPLEMENTED	------------------------------
	
	override def factory = EmailValidationFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return An access point to individual validation ids
	  */
	def id = DbEmailValidationId
	
	private def model = factory.model
	
	private def resendModel = EmailValidationResendModel
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param validationId Id of the targeted email validation
	  * @return An access point to that validation's data
	  */
	def apply(validationId: Int) = new SingleEmailValidationById(validationId)
	
	/**
	  * Finds an existing non-deprecated email validation record
	  * @param email Targeted email address
	  * @param purposeId Id of the purpose of this validation
	  * @param connection DB connection (implicit)
	  * @return Existing email validation record that is still active / open. None if there are no active / open
	  *         validation attempts at this time.
	  */
	def find(email: String, purposeId: Int)(implicit connection: Connection): Option[EmailValidation] =
		find(model.withEmail(email).withPurposeId(purposeId).toCondition)
	
	/**
	  * Finds an active / open, non-actualized, non-deprecated email validation that corresponds with the specified
	  * validation key and purpose. Performs the specified operation using this validation's data and possibly closes
	  * it afterwards.
	  * @param key An email validation key (usually send with the email)
	  * @param purposeId Id of the purpose for which this key was created / for which it is being used
	  *                  (these must match)
	  * @param f A function called if there existed a valid matching key for the specified purpose.
	  *          The function accepts email validation data and returns
	  *          1) a boolean indicating whether the validation should be closed and
	  *          2) additional final result
	  * @param connection DB Connection (implicit)
	  * @return The final result of the specified function on success. Failure if the specified key couldn't be
	  *         validated for the specified purpose (key was invalid, expired, for a different purpose or
	  *         already closed).
	  */
	def activateWithKey[A](key: String, purposeId: Int)(f: EmailValidation => (Boolean, A))
						  (implicit connection: Connection) =
		find(model.withKey(key).withPurposeId(purposeId).toCondition) match
		{
			// Case: Specified key is valid
			case Some(openValidation) =>
				// Performs the requested operation
				val (closeValidation, result) = f(openValidation)
				// May close / actualize this email validation
				if (closeValidation)
					model.withId(openValidation.id).nowActualized.update()
				Success(result)
			// Case: Specified key is invalid
			case None => Failure(new InvalidKeyException("Specified email activation key is either invalid or expired"))
		}
	
	/**
	  * @param resendKey A resend key
	  * @param connection DB Connection (implicit)
	  * @return An email validation matching that resend key
	  */
	def withResendKey(resendKey: String)(implicit connection: Connection) =
		find(model.withResendKey(resendKey).toCondition)
	
	/**
	  * Records an email validation resend using the specified resend key
	  * @param resendKey A resend key
	  * @param connection DB Connection (implicit)
	  * @param validator An email validator that will perform the actual resend operation (implicit)
	  * @return Either<br>
	  *         Right) Unit on success or<br>
	  *         Left) Proposed error status code and a description / message (failure state)
	  */
	def resendWith(resendKey: String)
				  (implicit connection: Connection, validator: EmailValidator): Either[(Status, String), Unit] =
	{
		withResendKey(resendKey) match
		{
			case Some(validation) =>
				// Records the resend (attempt) and checks whether maximum number of resends was exceeded
				apply(validation.id).recordResend(validator.maximumResendsPerValidation) match
				{
					case Success(_) => Right(validator.resend(validation))
					case Failure(error) => Left(Forbidden -> error.getMessage)
				}
			case None => Left(Unauthorized -> "Specified resend key is invalid or expired")
		}
	}
	
	/*
	  * Attempts to actualize / answer an email validation
	  * @param validationKey An email validation key
	  * @param connection DB Connection (implicit)
	  * @return A now validated email matching that key. Failure if the specified key was invalid or expired.
	  */
		/*
	def actualizeWith(validationKey: String)(implicit connection: Connection) =
	{
		find(model.withKey(validationKey).toCondition) match
		{
			case Some(validation) =>
				model.withId(validation.id).nowActualized.update()
				Success(validation.email)
			case None => Failure(new InvalidKeyException("Specified validation key is invalid or expired"))
		}
	}*/
	
	
	// NESTED	----------------------------------
	
	class SingleEmailValidationById(override val id: Int) extends SingleIntIdModelAccess[EmailValidation]
	{
		override def factory = DbEmailValidation.factory
		
		/**
		  * Records a validation message resend for this email validation case
		  * @param maxResends Maximum number of resends allowed per a single case (default = 5)
		  * @param connection DB Connection (implicit)
		  * @return The current number of resends for this email validation attempt. Failure if the maximum number of
		  *         resends would have been exceeded
		  */
		def recordResend(maxResends: Int = 5)(implicit connection: Connection) =
		{
			// Counts the current number of resends first to make sure a new resend is allowed
			val existingResendCount = connection(Count(resendModel.table) +
				Where(resendModel.withValidationId(id))).firstValue.getInt
			if (existingResendCount < maxResends)
			{
				resendModel.insert(id)
				Success(existingResendCount + 1)
			}
			else
				Failure(new TooManyRequestsException("Maximum number of email validation resends exceeded"))
		}
	}
}
