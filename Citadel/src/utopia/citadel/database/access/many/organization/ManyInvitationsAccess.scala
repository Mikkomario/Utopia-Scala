package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.InvitationFactory
import utopia.metropolis.model.stored.organization.Invitation

/**
  * A common trait for access points which target multiple Invitations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyInvitationsAccess extends ManyInvitationsAccessLike[Invitation]
{
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
}

