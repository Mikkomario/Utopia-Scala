package utopia.citadel.database.access.many.organization

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple InvitationResponses at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbInvitationResponses extends ManyInvitationResponsesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted InvitationResponses
	  * @return An access point to InvitationResponses with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbInvitationResponsesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbInvitationResponsesSubset(targetIds: Set[Int]) extends ManyInvitationResponsesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

