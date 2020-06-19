package utopia.metropolis.model.stored.description

import utopia.flow.datastructure.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.description.DescriptionLinkData
import utopia.metropolis.model.partial.description.DescriptionLinkData.FullDescriptionLinkData
import utopia.metropolis.model.stored.Stored

object DescriptionLink extends FromModelFactory[DescriptionLink]
{
	// ATTRIBUTES	-----------------------------
	
	private val baseSchema = ModelDeclaration(PropertyDeclaration("link_id", IntType))
	
	
	// IMPLEMENTED	-----------------------------
	
	override def apply(model: Model[Property]) = baseSchema.validate(model).toTry.flatMap { valid =>
		DescriptionLinkData.fullDescriptionLinkDataFactory(model).map { data =>
			DescriptionLink(valid("link_id"), data)
		}
	}
}

/**
  * Represents a stored description link of some type
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
case class DescriptionLink(id: Int, data: FullDescriptionLinkData)
	extends Stored[FullDescriptionLinkData] with ModelConvertible
{
	override def toModel = data.toModel + Constant("link_id", id)
}