package utopia.metropolis.model.partial.organization

import java.time.Instant

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, ModelValidationFailedException}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, InstantType, IntType, ModelConvertible, ModelType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.metropolis.model.enumeration.UserRole

import scala.util.{Failure, Success}

object InvitationData extends FromModelFactory[InvitationData]
{
	private val schema = ModelDeclaration("organization_id" -> IntType, "recipient" -> ModelType, "role_id" -> IntType,
		"expires" -> InstantType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		// Starting role must be parseable
		UserRole.forId(valid("role_id")).flatMap { role =>
			// Either recipient email or id is required
			val recipientModel = valid("recipient").getModel
			recipientModel("id").int.map { Right(_) }.orElse { recipientModel("email").string.map { Left(_) } } match
			{
				case Some(recipient) => Success(InvitationData(valid("organization_id"), recipient, role,
					valid("expires"), valid("sender_id")))
				case None => Failure(new ModelValidationFailedException(
					s"Either 'id' or 'email' required in recipient. Found $recipientModel"))
			}
		}
	}
}

/**
  * Contains basic information about an invitation to join an organization
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  * @param organizationId Id of the organization the user is invited to
  * @param recipient Either Right: Recipient user id or Left: Recipient user email address
  * @param startingRole Role assigned to the user in the organization, initially
  * @param expireTime Timestamp when the invitation will expire
  * @param creatorId Id of the user who created this invitation (optional)
  */
case class InvitationData(organizationId: Int, recipient: Either[String, Int], startingRole: UserRole,
						  expireTime: Instant, creatorId: Option[Int] = None) extends ModelConvertible
{
	// COMPUTED	----------------------------
	
	/**
	  * @return Recipient user id (None if defined by email)
	  */
	def recipientId = recipient.rightOption
	
	/**
	  * @return Recipient email address (None if defined by user id)
	  */
	def recipientEmail = recipient.leftOption
	
	/**
	  * @return Whether this invitation has expired already
	  */
	def hasExpired = expireTime <= Instant.now()
	
	
	// IMPLEMENTED	------------------------
	
	override def toModel =
	{
		val recipientModel = Model(Vector("id" -> recipientId, "email" -> recipientEmail))
		Model(Vector("organization_id" -> organizationId, "recipient" -> recipientModel, "role_id" -> startingRole.id,
			"expires" -> expireTime, "sender_id" -> creatorId))
	}
}
