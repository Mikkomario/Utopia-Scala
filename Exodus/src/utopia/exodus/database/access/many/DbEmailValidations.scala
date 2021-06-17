package utopia.exodus.database.access.many

import utopia.access.http.Status.Forbidden
import utopia.exodus.database.access.single.DbEmailValidation
import utopia.exodus.database.factory.user.EmailValidationFactory
import utopia.exodus.model.stored.EmailValidation
import utopia.exodus.util.EmailValidator
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess

import scala.util.{Failure, Success}

/**
  * Used for accessing & interacting with multiple email validations at a time
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
object DbEmailValidations extends ManyModelAccess[EmailValidation]
{
	// IMPLEMENTED	-----------------------------
	
	override def factory = EmailValidationFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	override protected def defaultOrdering = None
	
	
	// COMPUTED	---------------------------------
	
	private def model = factory.model
	
	
	// OTHER	----------------------------------
	
	/**
	  * Places a new open email validation attempt
	  * @param email Targeted email address
	  * @param purposeId Id representing the purpose of this validation
	  * @param ownerId Id of the user who owns or wants to own this email (optional)
	  * @param connection DB Connection (implicit)
	  * @param validator An email validator implementation to use (implicit)
	  * @return Either<br>
	  *         Right) Recorded email validation attempt or<br>
	  *         Left) Recommended response status and a descriptive message
	  */
	// TODO: Apply a limit based on origin ip address once that feature is available
	def place(email: String, purposeId: Int, ownerId: Option[Int] = None)
			 (implicit connection: Connection, validator: EmailValidator) =
	{
		// Checks whether there already exists an open validation attempt
		DbEmailValidation.find(email, purposeId) match
		{
			case Some(existing) =>
				// If there already existed a validation, records this one as a resend (unless limited)
				DbEmailValidation(existing.id).recordResend(validator.maximumResendsPerValidation) match
				{
					case Success(_) =>
						validator.resend(existing)
						Right(existing)
					case Failure(error) => Left(Forbidden -> error.getMessage)
				}
			case None =>
				// If not, sends and inserts a new one on success
				validator.validate(email, purposeId, ownerId).map { model.insert(_) }
		}
	}
}
