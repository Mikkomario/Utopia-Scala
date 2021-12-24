package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.InvitationWithResponseFactory
import utopia.metropolis.model.stored.organization.Invitation
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual Invitations, based on their id
  * @since 2021-10-23
  */
case class DbSingleInvitation(id: Int) extends UniqueInvitationAccess with SingleIntIdModelAccess[Invitation]
{
	// COMPUTED ------------------------------
	
	private def withResponseFactory = InvitationWithResponseFactory
	
	/**
	  * @return An access point to this invitation's response
	  */
	def response = DbInvitationResponse.forInvitationWithId(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return This invitation, including the possible response
	  */
	def withResponse(implicit connection: Connection) =
		withResponseFactory.find(condition)
}