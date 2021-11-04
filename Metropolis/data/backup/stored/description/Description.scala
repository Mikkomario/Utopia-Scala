package utopia.metropolis.model.stored.description

import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object Description extends StoredFromModelFactory[Description, DescriptionData]
{
	override def dataFactory = DescriptionData
}

/**
  * Represents a description stored in the database
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class Description(id: Int, data: DescriptionData) extends StoredModelConvertible[DescriptionData]
