package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object Invitation extends StoredFromModelFactory[Invitation, InvitationData]
{
	override def dataFactory = InvitationData
}

/**
  * Represents an organization invitation that has been stored to the DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class Invitation(id: Int, data: InvitationData) extends StoredModelConvertible[InvitationData]