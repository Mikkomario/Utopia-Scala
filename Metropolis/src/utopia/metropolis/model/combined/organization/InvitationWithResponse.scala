package utopia.metropolis.model.combined.organization

import utopia.flow.util.Extender
import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.organization.{Invitation, InvitationResponse}

/**
  * Combines Invitation with response data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class InvitationWithResponse(invitation: Invitation, response: Option[InvitationResponse]) 
	extends Extender[InvitationData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this Invitation in the database
	  */
	def id = invitation.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = invitation.data
}

