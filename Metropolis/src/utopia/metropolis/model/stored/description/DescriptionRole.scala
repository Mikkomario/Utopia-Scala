package utopia.metropolis.model.stored.description

import utopia.metropolis.model.partial.description.DescriptionRoleData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object DescriptionRole extends StoredFromModelFactory[DescriptionRole, DescriptionRoleData]
{
	override def dataFactory = DescriptionRoleData
}

/**
  * Represents a DescriptionRole that has already been stored in the database
  * @param id id of this DescriptionRole in the database
  * @param data Wrapped DescriptionRole data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class DescriptionRole(id: Int, data: DescriptionRoleData) 
	extends StoredModelConvertible[DescriptionRoleData]

