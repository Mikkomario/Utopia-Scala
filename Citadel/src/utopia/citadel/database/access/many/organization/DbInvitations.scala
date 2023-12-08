package utopia.citadel.database.access.many.organization

import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.nosql.view.{NonDeprecatedView, UnconditionalView}

/**
  * The root access point when targeting multiple Invitations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbInvitations extends ManyInvitationsAccess with NonDeprecatedView[Invitation]
{
	// COMPUTED --------------------
	
	/**
	  * @return An access point to invitations which are not limited to non-expired invitations
	  */
	def currentAndPast = DbCurrentAndPastInvitations
	
	
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Invitations
	  * @return An access point to Invitations with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbInvitationsSubset(ids)
	
	
	// NESTED	--------------------
	
	object DbCurrentAndPastInvitations extends ManyInvitationsAccess with UnconditionalView
	
	class DbInvitationsSubset(targetIds: Set[Int]) extends ManyInvitationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

