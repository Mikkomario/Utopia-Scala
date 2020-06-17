package utopia.metropolis.model.stored.description

import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a description stored in the database
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  */
case class Description(id: Int, data: DescriptionData) extends StoredModelConvertible[DescriptionData]
