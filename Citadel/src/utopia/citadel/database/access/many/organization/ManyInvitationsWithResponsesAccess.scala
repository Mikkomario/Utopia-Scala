package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.organization.ManyInvitationsWithResponsesAccess.SubAccess
import utopia.citadel.database.factory.organization.InvitationWithResponseFactory
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyInvitationsWithResponsesAccess
{
	private class SubAccess(override val parent: ManyModelAccess[InvitationWithResponse],
	                        override val filterCondition: Condition)
		extends ManyInvitationsWithResponsesAccess with SubView
}

/**
  * A common trait for access points that return invitations with responses included
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyInvitationsWithResponsesAccess
	extends ManyInvitationsAccessLike[InvitationWithResponse, ManyInvitationsWithResponsesAccess]
{
	// COMPUTED ---------------------------------------
	
	/**
	  * @return Model used for interacting with invitation responses
	  */
	protected def responseModel = InvitationResponseModel
	
	/**
	  * @return An access point to blocked invitations
	  */
	def blocked = filter(responseModel.blocked.toCondition)
	/**
	  * @return An access point to invitations which are open but without response
	  */
	def pending =
		filter(model.nonDeprecatedCondition && factory.notLinkedCondition)
	
	
	// IMPLEMENTED  -----------------------------------
	
	override def factory = InvitationWithResponseFactory
	override protected def defaultOrdering = None
	
	override protected def _filter(condition: Condition): ManyInvitationsWithResponsesAccess =
		new SubAccess(this, condition)
}
