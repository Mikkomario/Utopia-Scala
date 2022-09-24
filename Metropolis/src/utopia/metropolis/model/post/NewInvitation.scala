package utopia.metropolis.model.post

import utopia.flow.collection.template.typeless
import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.datastructure.template
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.mutable.{IntType, StringType}
import utopia.flow.generic.model.template.{Model, ModelConvertible, Property}
import utopia.flow.time.Days
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.error.IllegalPostModelException
import utopia.metropolis.util.MetropolisRegex

import scala.util.{Failure, Success}

object NewInvitation extends FromModelFactory[NewInvitation]
{
	private val schema = ModelDeclaration("recipient_email" -> StringType, "role_id" -> IntType)
	
	override def apply(model: Model[Property]) = {
		schema.validate(model).toTry.flatMap { model =>
			// Makes sure the email address formatting is correct
			val emailAddress = model("recipient_email").getString
			if (MetropolisRegex.email(emailAddress)) {
				// Also makes sure duration is positive
				val rawDuration = model("duration_days").int
				if (rawDuration.forall { _ > 0 })
					Success(NewInvitation(emailAddress, model("role_id"), model("message"),
						rawDuration.getOrElse(7).days))
				else
					Failure(new IllegalPostModelException(s"${rawDuration.get} days is an invalid invitation duration"))
			}
			else
				Failure(new IllegalPostModelException(s"'$emailAddress' is not a valid email address"))
		}
	}
}

/**
  * Used for creating new invitations on server side
  * @author Mikko Hilpinen
  * @since 5.5.2020, v1
  * @param recipientEmail Email address of the invitation recipient
  * @param startingRoleId Id of the role given to the recipient upon invitation accept
  * @param message Message attached to this invitation (default = empty = no message)
  * @param duration Invitation validity period length
  */
case class NewInvitation(recipientEmail: String, startingRoleId: Int, message: String = "", duration: Days = Days(7))
	extends ModelConvertible
{
	// COMPUTED	--------------------------------
	
	/**
	  * Checks the parameters in this post model
	  * @return Success(this) if this model is valid. Failure otherwise.
	  */
	@deprecated("Moved these checks to from model parsing instead", "v2.1")
	def validated =
	{
		if (!recipientEmail.contains("@"))
			Failure(new IllegalPostModelException("recipient_email must be a valid email address"))
		else if (duration.length <= 0)
			Failure(new IllegalPostModelException("duration_days must be positive"))
		else
			Success(this)
	}
	
	
	// IMPLEMENTED	----------------------------
	
	override def toModel = Model(Vector("recipient_email" -> recipientEmail,
		"role_id" -> startingRoleId, "message" -> message.notEmpty, "duration_days" -> duration.length))
}
