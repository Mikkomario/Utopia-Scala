package utopia.metropolis.model.partial.organization

import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Contains basic data about an invitation response
  * @author Mikko Hilpinen
  * @since 4.5.2020, v2
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
