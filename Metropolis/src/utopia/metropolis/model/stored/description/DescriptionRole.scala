package utopia.metropolis.model.stored.description

import utopia.metropolis.model.partial.description.DescriptionRoleData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object DescriptionRole extends StoredFromModelFactory[DescriptionRole, DescriptionRoleData]
{
	override def dataFactory = DescriptionRoleData
}

/**
  * Represents a recorded description role
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
case class DescriptionRole(id: Int, data: DescriptionRoleData) extends StoredModelConvertible[DescriptionRoleData]
