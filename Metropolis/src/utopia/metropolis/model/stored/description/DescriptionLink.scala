package utopia.metropolis.model.stored.description

import utopia.metropolis.model.partial.description.DescriptionLinkData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a stored link between a description and the item it describes
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
case class DescriptionLink(id: Int, data: DescriptionLinkData) extends StoredModelConvertible[DescriptionLinkData]
