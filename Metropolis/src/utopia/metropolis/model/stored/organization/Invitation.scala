package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.InvitationData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object Invitation extends StoredFromModelFactory[Invitation, InvitationData]
{
	override def dataFactory = InvitationData
}

/**
  * Represents a Invitation that has already been stored in the database
  * @param id id of this Invitation in the database
  * @param data Wrapped Invitation data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class Invitation(id: Int, data: InvitationData) extends StoredModelConvertible[InvitationData]

