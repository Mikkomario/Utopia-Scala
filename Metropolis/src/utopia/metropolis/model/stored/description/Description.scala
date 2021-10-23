package utopia.metropolis.model.stored.description

import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object Description extends StoredFromModelFactory[Description, DescriptionData]
{
	override def dataFactory = DescriptionData
}

/**
  * Represents a Description that has already been stored in the database
  * @param id id of this Description in the database
  * @param data Wrapped Description data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class Description(id: Int, data: DescriptionData) extends StoredModelConvertible[DescriptionData]

