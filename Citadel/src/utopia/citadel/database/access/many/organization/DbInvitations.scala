package utopia.citadel.database.access.many.organization

import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple Invitations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbInvitations extends ManyInvitationsAccess with NonDeprecatedView[Invitation]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Invitations
	  * @return An access point to Invitations with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbInvitationsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbInvitationsSubset(targetIds: Set[Int]) extends ManyInvitationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

