package utopia.citadel.database.factory.organization

import utopia.metropolis.model.combined.organization.InvitationWithResponse
import utopia.metropolis.model.stored.organization.{Invitation, InvitationResponse}
import utopia.vault.nosql.factory.row.linked.PossiblyCombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading InvitationWithResponses from the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object InvitationWithResponseFactory 
	extends PossiblyCombiningFactory[InvitationWithResponse, Invitation, InvitationResponse] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = InvitationResponseFactory
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition
	
	override def parentFactory = InvitationFactory
	
	override def apply(invitation: Invitation, response: Option[InvitationResponse]) = 
		InvitationWithResponse(invitation, response)
}

