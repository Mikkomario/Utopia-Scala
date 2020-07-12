package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.InvitationResponseData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object InvitationResponse extends StoredFromModelFactory[InvitationResponse, InvitationResponseData]
{
	override def dataFactory = InvitationResponseData
}

/**
  * Represents an invitation response that has been stored to DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class InvitationResponse(id: Int, data: InvitationResponseData)
	extends StoredModelConvertible[InvitationResponseData]
