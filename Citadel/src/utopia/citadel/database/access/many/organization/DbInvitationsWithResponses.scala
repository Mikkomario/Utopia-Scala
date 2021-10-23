package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.InvitationWithResponseFactory
import utopia.citadel.database.model.organization.InvitationResponseModel
import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing multiple invitations at once, including their responses
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
object DbInvitationsWithResponses extends ManyInvitationsAccessLike[InvitationWithResponse] with UnconditionalView
{
	// COMPUTED ---------------------------------------
	
	private def responseModel = InvitationResponseModel
	
	/**
	  * @return An access point to blocked invitations
	  */
	def blocked = filter(responseModel.blocked.toCondition)
	
	/**
	  * @return An access point to invitations which are open but without response
	  */
	def pending = filter(model.nonDeprecatedCondition && factory.notLinkedCondition)
	
	
	// IMPLEMENTED  -----------------------------------
	
	override def factory = InvitationWithResponseFactory
	
	override protected def defaultOrdering = Some(factory.parentFactory.defaultOrdering)
}
