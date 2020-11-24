package utopia.exodus.database.access.single

import utopia.exodus.database.access.id.EmailValidationId
import utopia.exodus.database.factory.user.EmailValidationFactory
import utopia.exodus.database.model.user.EmailValidationResendModel
import utopia.exodus.model.error.{InvalidKeyException, TooManyRequestsException}
import utopia.exodus.model.stored.EmailValidation
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleIdModelAccess, SingleModelAccess}
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
	def id = EmailValidationId
	
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
	  * Records an email validation resend using the specified resend key
	  * @param resendKey A resend key
	  * @param maxResends Maximum number of resends allowed in total for the targeted validation
	  * @param connection DB Connection (implicit)
	  * @return Current number of resends for this validation. Failure if the maximum number of resends was exceeded or
	  *         if the specified resend key was invalid.
	  */
	def resendWith(resendKey: String, maxResends: Int = 5)(implicit connection: Connection) =
	{
		id.forResendKey(resendKey) match
		{
			case Some(validationId) => apply(validationId).recordResend(maxResends)
			case None => Failure(new InvalidKeyException("Specified resend key is invalid or expired"))
		}
	}
	
	/**
	  * Attempts to actualize / answer an email validation
	  * @param validationKey An email validation key
	  * @param connection DB Connection (implicit)
	  * @return A now validated email matching that key. Failure if the specified key was invalid or expired.
	  */
	def actualizeWith(validationKey: String)(implicit connection: Connection) =
	{
		find(model.withKey(validationKey).toCondition) match
		{
			case Some(validation) =>
				model.withId(validation.id).nowActualized.update()
				Success(validation.email)
			case None => Failure(new InvalidKeyException("Specified validation key is invalid or expired"))
		}
	}
	
	
	// NESTED	----------------------------------
	
	class SingleEmailValidationById(id: Int) extends SingleIdModelAccess[EmailValidation](id, DbEmailValidation.factory)
	{
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
