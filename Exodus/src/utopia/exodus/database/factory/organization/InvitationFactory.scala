package utopia.exodus.database.factory.organization

import utopia.exodus.database.Tables
import utopia.flow.datastructure.template.{Model, Property}
import utopia.metropolis.model.error.NoDataFoundException
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.nosql.factory.FromRowModelFactory

import scala.util.{Failure, Success}

/**
  * Used for reading organization invitations from the DB
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object InvitationFactory extends FromRowModelFactory[Invitation]
{
	// IMPLEMENTED	---------------------------
	
	override def table = Tables.organizationInvitation
	
	override def apply(model: Model[Property]) = table.requirementDeclaration.validate(model).toTry.flatMap { valid =>
		// Either recipient id or recipient email must be provided
		val recipient =
		{
			valid("recipientId").int.map { id => Success(Right(id)) }.orElse { valid("recipientEmail").string.map {
				email => Success(Left(email)) } }.getOrElse(Failure(new NoDataFoundException(
				s"Didn't find recipientId or recipientEmail from $valid")))
		}
		recipient.map { recipient =>
			Invitation(valid("id").getInt, InvitationData(valid("organizationId").getInt, recipient,
				valid("startingRoleId").getInt, valid("expiresIn").getInstant, valid("creatorId").int))
		}
	}
}
