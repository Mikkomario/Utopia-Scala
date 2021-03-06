package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object Membership extends StoredFromModelFactory[Membership, MembershipData]
{
	override def dataFactory = MembershipData
}

/**
  * Represents a stored organization membership
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class Membership(id: Int, data: MembershipData) extends StoredModelConvertible[MembershipData]
