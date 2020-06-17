package utopia.metropolis.model.stored.description

import utopia.metropolis.model.partial.description.DescriptionLinkData.FullDescriptionLinkData
import utopia.metropolis.model.stored.StoredModelConvertible

/**
  * Represents a stored description link of some type
  * @author Mikko Hilpinen
  * @since 16.5.2020, v2
  */
case class DescriptionLink(id: Int, data: FullDescriptionLinkData) extends StoredModelConvertible[FullDescriptionLinkData]