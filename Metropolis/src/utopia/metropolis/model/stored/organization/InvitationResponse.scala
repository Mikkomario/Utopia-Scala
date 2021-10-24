package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object InvitationResponse extends StoredFromModelFactory[InvitationResponse, InvitationResponseData]
{
	override def dataFactory = InvitationResponseData
}

/**
  * Represents a InvitationResponse that has already been stored in the database
  * @param id id of this InvitationResponse in the database
  * @param data Wrapped InvitationResponse data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class InvitationResponse(id: Int, data: InvitationResponseData) 
	extends StoredModelConvertible[InvitationResponseData]

