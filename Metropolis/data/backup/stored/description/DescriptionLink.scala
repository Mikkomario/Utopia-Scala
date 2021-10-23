package utopia.metropolis.model.stored.description

import utopia.flow.datastructure.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.Now
import utopia.metropolis.model.partial.description.DescriptionLinkDataOld
import utopia.metropolis.model.partial.description.DescriptionLinkDataOld.FullDescriptionLinkData
import utopia.metropolis.model.stored.Stored

import java.time.Instant

object DescriptionLink extends FromModelFactory[DescriptionLinkOld]
{
	// ATTRIBUTES	-----------------------------
	
	private val baseSchema = ModelDeclaration(PropertyDeclaration("link_id", IntType))
	
	
	// IMPLEMENTED	-----------------------------
	
	override def apply(model: Model[Property]) = baseSchema.validate(model).toTry.flatMap { valid =>
		DescriptionLinkDataOld.fullDescriptionLinkDataFactory(model).map { data =>
			DescriptionLinkOld(valid("link_id"), data)
		}
	}
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Creates a new description link
	  * @param id Description link id
	  * @param targetId Description target id
	  * @param description Description
	  * @param created Link creation time
	  * @return A new description link
	  */
	def apply(id: Int, targetId: Int, description: Description, created: Instant = Now): DescriptionLinkOld =
		apply(id, DescriptionLinkDataOld(targetId, description, created))
}

/**
  * Represents a stored description link of some type
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
// TODO: Refactor by separating the description data from here (add a separate combined model for that)
case class DescriptionLink(id: Int, data: FullDescriptionLinkData)
	extends Stored[FullDescriptionLinkData] with ModelConvertible
{
	override def toModel = data.toModel + Constant("link_id", id)
}