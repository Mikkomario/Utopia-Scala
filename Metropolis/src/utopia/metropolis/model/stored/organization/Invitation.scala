package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents an organization invitation that has been stored to the DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v2
  */
case class Invitation(id: Int, data: InvitationData) extends StoredModelConvertible[InvitationData]