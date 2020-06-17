package utopia.metropolis.model.partial.description

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.description.Description

object DescriptionLinkData
{
	/**
	  * Description link data for descriptions that haven't been inserted to database yet
	  */
	type PartialDescriptionLinkData = DescriptionLinkData[DescriptionData]
	
	/**
	  * Description link data for stored descriptions
	  */
	type FullDescriptionLinkData = DescriptionLinkData[Description]
}

/**
  * Contains basic data for a description link
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  * @param targetId Id of the described target
  * @param description Description of the device
  * @tparam D Type of description contained within this data
  */
case class DescriptionLinkData[+D <: ModelConvertible](targetId: Int, description: D) extends ModelConvertible
{
	override def toModel = description.toModel + Constant("target_id", targetId)
}
