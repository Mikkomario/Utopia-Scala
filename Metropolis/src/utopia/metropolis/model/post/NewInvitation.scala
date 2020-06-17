package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.TimeExtensions._
import utopia.metropolis.model.enumeration.UserRole
import utopia.metropolis.model.error.IllegalPostModelException

import scala.util.{Failure, Success}

object NewInvitation extends FromModelFactory[NewInvitation]
{
	private val schema = ModelDeclaration("recipient_email" -> StringType, "role_id" -> IntType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		// Role must be parseable
		UserRole.forId(valid("role_id").getInt).map { role => NewInvitation(valid("recipient_email").getString, role,
			valid("duration_days").intOr(7)) }
	}
}

/**
  * Used for creating new invitations on server side
  * @author Mikko Hilpinen
  * @since 5.5.2020, v2
  * @param recipientEmail Email address of the invitation recipient
  * @param startingRole Role given to the recipient upon invitation accept
  * @param durationDays Invitation validity period duration in days
  */
case class NewInvitation(recipientEmail: String, startingRole: UserRole, durationDays: Int = 7) extends ModelConvertible
{
	// COMPUTED	--------------------------------
	
	/**
	  * @return A time period that describes how long this invitation should be valid
	  */
	def validityPeriod = durationDays.days
	
	/**
	  * Checks the parameters in this post model
	  * @return Success(this) if this model is valid. Failure otherwise.
	  */
	def validated =
	{
		if (!recipientEmail.contains("@"))
			Failure(new IllegalPostModelException("recipient_email must be a valid email address"))
		else if (durationDays <= 0)
			Failure(new IllegalPostModelException("duration_days must be positive"))
		else
			Success(this)
	}
	
	
	// IMPLEMENTED	----------------------------
	
	override def toModel = Model(Vector("recipient_email" -> recipientEmail,
		"role_id" -> startingRole.id, "duration_days" -> durationDays))
}
