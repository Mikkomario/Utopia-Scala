package utopia.metropolis.model.stored.organization

import utopia.metropolis.model.partial.organization.DeletionCancelData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Stored organization deletion cancellation
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
case class DeletionCancel(id: Int, data: DeletionCancelData) extends StoredModelConvertible[DeletionCancelData]
