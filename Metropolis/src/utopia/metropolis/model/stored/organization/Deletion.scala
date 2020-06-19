package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.DeletionData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a stored organization deletion attempt
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
case class Deletion(id: Int, data: DeletionData) extends StoredModelConvertible[DeletionData]
