package utopia.exodus.database.access.many

import java.time.Instant

import utopia.exodus.database.access.single.DbEmailValidation
import utopia.exodus.database.factory.user.EmailValidationFactory
import utopia.exodus.model.partial.EmailValidationData
import utopia.exodus.model.stored.EmailValidation
import utopia.exodus.util.UuidGenerator
import utopia.flow.util.TimeExtensions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess

import scala.concurrent.duration.FiniteDuration
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
	
	
	// COMPUTED	---------------------------------
	
	private def model = factory.model
	
	
	// OTHER	----------------------------------
	
	/**
	  * Places a new open email validation attempt
	  * @param email Targeted email address
	  * @param purposeId Id representing the purpose of this validation
	  * @param duration The duration of time this validation should remain actionable (default = 15 minutes)
	  * @param maximumNumberOfResends The maximum numbers of email resends allowed for each validation case
	  * @param connection DB Connection (implicit)
	  * @param uuidGenerator A generator that provides new unique action and resend keys (implicit)
	  * @return A new or existing email validation attempt. Failure if maximum number of resends would have been
	  *         exceeded.
	  */
	// TODO: Apply a limit based on origin ip address once that feature is available
	def place(email: String, purposeId: Int, duration: FiniteDuration = 15.minutes, maximumNumberOfResends: Int = 5)
			 (implicit connection: Connection, uuidGenerator: UuidGenerator) =
	{
		// Checks whether there already exists an open validation attempt
		DbEmailValidation.find(email, purposeId) match
		{
			case Some(existing) =>
				// If there already existed a validation, records this one as a resend (unless limited)
				DbEmailValidation(existing.id).recordResend(maximumNumberOfResends) match
				{
					case Success(_) => Success(existing)
					case Failure(error) => Failure(error)
				}
			case None =>
				// If not, inserts a new one
				val now = Instant.now()
				Success(model.insert(EmailValidationData(purposeId, email, uuidGenerator.next(), uuidGenerator.next(),
					now + duration, now)))
		}
	}
}
