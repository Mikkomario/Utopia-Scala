package utopia.metropolis.model.partial.description

import utopia.flow.collection.value.typeless.Model
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._

/**
  * Contains basic information concerning a link between a description and its target
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  * @param targetId Id of the description target
  * @param descriptionId Id of the linked description
  */
case class DescriptionLinkData(targetId: Int, descriptionId: Int) extends ModelConvertible
{
	override def toModel = Model(Vector("target_id" -> targetId, "description_id" -> descriptionId))
}
