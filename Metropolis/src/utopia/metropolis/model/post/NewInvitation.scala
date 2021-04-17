package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.error.IllegalPostModelException

import scala.util.{Failure, Success}

object NewInvitation extends FromModelFactoryWithSchema[NewInvitation]
{
	val schema = ModelDeclaration("recipient_email" -> StringType, "role_id" -> IntType)
	
	override protected def fromValidatedModel(model: Model[Constant]) = NewInvitation(model("recipient_email"),
		model("role_id"), model("duration_days").intOr(7))
}

/**
  * Used for creating new invitations on server side
  * @author Mikko Hilpinen
  * @since 5.5.2020, v1
  * @param recipientEmail Email address of the invitation recipient
  * @param startingRoleId Id of the role given to the recipient upon invitation accept
  * @param durationDays Invitation validity period duration in days
  */
case class NewInvitation(recipientEmail: String, startingRoleId: Int, durationDays: Int = 7) extends ModelConvertible
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
		"role_id" -> startingRoleId, "duration_days" -> durationDays))
}
