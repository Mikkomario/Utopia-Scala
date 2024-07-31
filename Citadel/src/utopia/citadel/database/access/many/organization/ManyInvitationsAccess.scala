package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.InvitationFactory
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.nosql.view.ViewFactory
import utopia.vault.sql.Condition

object ManyInvitationsAccess extends ViewFactory[ManyInvitationsAccess]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	override def apply(condition: Condition): ManyInvitationsAccess = new _ManyInvitationsAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _ManyInvitationsAccess(condition: Condition) extends ManyInvitationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple Invitations at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyInvitationsAccess extends ManyInvitationsAccessLike[Invitation, ManyInvitationsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to invitations with responses
	  */
	def withResponses = DbInvitationsWithResponses.filter(accessCondition)
	
	/**
	  * An access point to invitations that don't have a response yet (read as invitations with responses)
	  */
	def notAnswered = withResponses.notAnswered
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = InvitationFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyInvitationsAccess = ManyInvitationsAccess(condition)
}

