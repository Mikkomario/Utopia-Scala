package utopia.metropolis.model.partial.organization

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{BooleanType, FromModelFactoryWithSchema, IntType, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object InvitationResponseData extends FromModelFactoryWithSchema[InvitationResponseData]
{
	// ATTRIBUTES	--------------------------
	
	override val schema = ModelDeclaration("invitation_id" -> IntType, "was_accepted" -> BooleanType,
		"creator_id" -> IntType)
	
	
	// IMPLEMENTED	--------------------------
	
	override protected def fromValidatedModel(model: Model) = InvitationResponseData(model("invitation_id"),
		model("was_accepted"), model("was_blocked"), model("creator_id"))
}

/**
  * Contains basic data about an invitation response
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  * @param invitationId Id of the invitation this response is for
  * @param wasAccepted Whether the invitation was accepted
  * @param wasBlocked Whether future invitations were blocked
  * @param creatorId Id of the user who accepted or rejected the invitation
  */
case class InvitationResponseData(invitationId: Int, wasAccepted: Boolean, wasBlocked: Boolean, creatorId: Int)
	extends ModelConvertible
{
	override def toModel = Model(Vector("invitation_id" -> invitationId,
		"was_accepted" -> wasAccepted, "was_blocked" -> wasBlocked, "creator_id" -> creatorId))
}
