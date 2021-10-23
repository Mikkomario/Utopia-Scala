package utopia.metropolis.model.partial.description

import java.time.Instant
import utopia.flow.datastructure.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.Now
import utopia.metropolis.model.stored.description.Description

object DescriptionLinkData
{
	// TYPES	--------------------------
	
	/**
	  * Description link data for descriptions that haven't been inserted to database yet
	  */
	type PartialDescriptionLinkData = DescriptionLinkDataOld[DescriptionData]
	
	/**
	  * Description link data for stored descriptions
	  */
	type FullDescriptionLinkData = DescriptionLinkDataOld[Description]
	
	
	// ATTRIBUTES	----------------------
	
	/**
	  * A factory used for parsing partial description link data elements from model data
	  */
	val partialDescriptionLinkDataFactory: FromModelFactory[PartialDescriptionLinkData] =
		new DescriptionLinkDataFromModelFactory[DescriptionData](DescriptionData)
	
	/**
	  * A factory used for parsing full description link data elements from model data
	  */
	val fullDescriptionLinkDataFactory: FromModelFactory[FullDescriptionLinkData] =
		new DescriptionLinkDataFromModelFactory[Description](Description)
	
	private val commonSchema = ModelDeclaration(PropertyDeclaration("target_id", IntType))
	
	
	// NESTED	--------------------------
	
	private class DescriptionLinkDataFromModelFactory[+D <: ModelConvertible](descriptionFactory: FromModelFactory[D])
		extends FromModelFactory[DescriptionLinkDataOld[D]]
	{
		override def apply(model: Model[Property]) = commonSchema.validate(model).toTry.flatMap { valid =>
			descriptionFactory(valid).map { description => DescriptionLinkDataOld[D](valid("target_id"), description,
				valid("link_created")) }
		}
	}
}

/**
  * Contains basic data for a description link
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param targetId Id of the described target
  * @param description Description of the device
  * @tparam D Type of description contained within this data
  */
case class DescriptionLinkData[+D <: ModelConvertible](targetId: Int, description: D, created: Instant = Now)
	extends ModelConvertible
{
	override def toModel = description.toModel + Constant("target_id", targetId) +
		Constant("link_created", created)
}
