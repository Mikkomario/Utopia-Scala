package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StyledStoredModelConvertible}

object Membership extends StoredFromModelFactory[Membership, MembershipData]
{
	override def dataFactory = MembershipData
}

/**
  * Represents a Membership that has already been stored in the database
  * @param id id of this Membership in the database
  * @param data Wrapped Membership data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class Membership(id: Int, data: MembershipData) extends StyledStoredModelConvertible[MembershipData]
{
	override protected def includeIdInSimpleModel = false
}
