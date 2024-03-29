package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.organization.ManyInvitationsAccess.SubAccess
import utopia.citadel.database.factory.organization.InvitationFactory
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyInvitationsAccess
{
	private class SubAccess(override val parent: ManyModelAccess[Invitation], override val filterCondition: Condition)
		extends ManyInvitationsAccess with SubView
}

/**
  * A common trait for access points which target multiple Invitations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyInvitationsAccess extends ManyInvitationsAccessLike[Invitation, ManyInvitationsAccess]
{
	// COMPUTED ------------------------
	
	/**
	  * @return An access point to invitations with responses
	  */
	def withResponses = accessCondition match
	{
		case Some(condition) => DbInvitationsWithResponses.filter(condition)
		case None => DbInvitationsWithResponses
	}
	/**
	  * @return An access point to invitations that don't have a response yet (read as invitations with responses)
	  */
	def notAnswered = withResponses.notAnswered
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = InvitationFactory
	
	override protected def _filter(condition: Condition): ManyInvitationsAccess = new SubAccess(this, condition)
}

