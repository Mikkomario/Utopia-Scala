package utopia.metropolis.model.combined.organization

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.Extender
import utopia.metropolis.model.stored.organization.{Invitation, InvitationResponse}

/**
  * An extender to standard invitation model that also contains the response to that invitation (if present)
  * @author Mikko Hilpinen
  * @since 4.5.2020, v2
  */
case class InvitationWithResponse(invitation: Invitation, response: InvitationResponse) extends Extender[Invitation]
	with ModelConvertible
{
	// COMPUTED	----------------------------------
	
	/**
	  * @return Id of the user who received this invitation
	  */
	def recipientId = response.creatorId
	
	
	// IMPLEMENTED	------------------------------
	
	override def wrapped = invitation
	
	override def toModel =
	{
		// Includes the response model
		val base = wrapped.toModel
		val responseModel = response.toModel
		base + Constant("response", responseModel)
	}
}
