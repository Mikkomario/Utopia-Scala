package utopia.metropolis.model.combined.description

import utopia.flow.collection.template.typeless
import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.datastructure.template
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.mutable.IntType
import utopia.flow.generic.model.template.{Model, ModelConvertible, Property}
import utopia.flow.view.template.Extender
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.stored.description.Description

object LinkedDescription extends FromModelFactory[LinkedDescription]
{
	// ATTRIBUTES   ------------------------------
	
	private val linkSchema = ModelDeclaration("link_id" -> IntType, "target_id" -> IntType)
	
	
	// IMPLEMENTED  ------------------------------
	
	override def apply(model: Model[Property]) =
		linkSchema.validate(model).toTry.flatMap { model =>
			Description(model).map { description =>
				LinkedDescription(description, model("link_id"), model("target_id"))
			}
		}
}

/**
  * Adds link information to a description
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  * @param description The wrapped description
  * @param linkId Id of the link between this description and the described item
  * @param targetId Id of the described item
  */
case class LinkedDescription(description: Description, linkId: Int, targetId: Int)
	extends Extender[DescriptionData] with ModelConvertible
{
	/**
	 * @return Id of this description
	 */
	def id = description.id
	
	override def wrapped = description
	
	override def toModel = description.toModel ++
		Model(Vector("link_id" -> linkId, "target_id" -> targetId))
}
