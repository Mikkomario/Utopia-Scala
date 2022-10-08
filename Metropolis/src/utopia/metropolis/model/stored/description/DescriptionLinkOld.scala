package utopia.metropolis.model.stored.description

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.time.Now
import utopia.metropolis.model.partial.description.DescriptionLinkDataOld
import utopia.metropolis.model.partial.description.DescriptionLinkDataOld.FullDescriptionLinkData
import utopia.metropolis.model.stored.Stored

import java.time.Instant

@deprecated("Replaced with a new version", "v2.0")
object DescriptionLinkOld extends FromModelFactory[DescriptionLinkOld]
{
	// ATTRIBUTES	-----------------------------
	
	private val baseSchema = ModelDeclaration(PropertyDeclaration("link_id", IntType))
	
	
	// IMPLEMENTED	-----------------------------
	
	override def apply(model: ModelLike[Property]) = baseSchema.validate(model).toTry.flatMap { valid =>
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
@deprecated("Replaced with a new version", "v2.0")
case class DescriptionLinkOld(id: Int, data: FullDescriptionLinkData)
	extends Stored[FullDescriptionLinkData] with ModelConvertible
{
	override def toModel = data.toModel + Constant("link_id", id)
}