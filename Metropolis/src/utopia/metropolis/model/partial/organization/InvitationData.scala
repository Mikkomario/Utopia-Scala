package utopia.metropolis.model.partial.organization

import java.time.Instant
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, InstantType, IntType, ModelConvertible, ModelType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

object InvitationData extends FromModelFactory[InvitationData]
{
	private val schema = ModelDeclaration("organization_id" -> IntType, "recipient_id" -> ModelType,
		"starting_role_id" -> IntType, "expires" -> InstantType)
	
	override def apply(model: template.Model[Property]) =
		schema.validate(model).toTry.map { model =>
			InvitationData(model("organization_id"), model("starting_role_id"), model("expires"),
				model("recipient_id"), model("recipient_email"), model("message"), model("sender_id"),
				model("created"))
		}
		/*.flatMap { valid =>
		// Either recipient email or id is required
		val recipientModel = valid("recipient").getModel
		recipientModel("id").int.map { Right(_) }.orElse { recipientModel("email").string.map { Left(_) } } match
		{
			case Some(recipient) => Success(InvitationData(valid("organization_id"), recipient, valid("role_id"),
				valid("expires"), valid("sender_id")))
			case None => Failure(new ModelValidationFailedException(
				s"Either 'id' or 'email' required in recipient. Found $recipientModel"))
		}
	}*/
}

/**
  * Represents an invitation to join an organization
  * @param organizationId Id of the organization which the recipient is invited to join
  * @param startingRoleId The role the recipient will have in the organization initially if they join
  * @param expires Time when this Invitation expires / becomes invalid
  * @param recipientId Id of the invited user, if known
  * @param recipientEmail Email address of the invited user / the email address where this invitation is sent to
  * @param message Message written by the sender to accompany this invitation
  * @param senderId Id of the user who sent this invitation, if still known
  * @param created Time when this invitation was created / sent
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class InvitationData(organizationId: Int, startingRoleId: Int, expires: Instant, 
	recipientId: Option[Int] = None, recipientEmail: Option[String] = None, message: Option[String] = None, 
	senderId: Option[Int] = None, created: Instant = Now) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this Invitation is no longer valid because it has expired
	  */
	def hasExpired = expires <= Now
	
	/**
	  * Whether this Invitation is still valid (hasn't expired yet)
	  */
	def isValid = !hasExpired
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("organization_id" -> organizationId, "starting_role_id" -> startingRoleId, 
			"expires" -> expires, "recipient_id" -> recipientId, "recipient_email" -> recipientEmail, 
			"message" -> message, "sender_id" -> senderId, "created" -> created))
}

