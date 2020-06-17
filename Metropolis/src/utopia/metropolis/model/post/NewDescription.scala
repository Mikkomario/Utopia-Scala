package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.{FromModelFactory, IntType, StringType, VectorType}
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.enumeration.DescriptionRole

object NewDescription extends FromModelFactory[NewDescription]
{
	// ATTRIBUTES	----------------------------
	
	private val descriptionSchema = ModelDeclaration("role_id" -> IntType, "text" -> StringType)
	private val schema = ModelDeclaration("language_id" -> IntType, "descriptions" -> VectorType)
	
	
	// IMPLEMENTED	----------------------------
	
	override def apply(model: Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		// All specified descriptions must adhere to description schema and each referenced role id must be valid
		val descriptions = valid("descriptions").getVector.tryMap { dv => descriptionSchema.validate(dv.getModel).toTry.flatMap { dm =>
			DescriptionRole.forId(dm("role_id")).map { role => role -> dm("text").getString }
		} }
		descriptions.map { validDescriptions =>
			// NB: If there are multiple descriptions for a single role, only one of those is preserved
			NewDescription(valid("language_id"), validDescriptions.toMap)
		}
	}
}

/**
  * Used for posting new / updated descriptions for organizations, devices, etc.
  * @author Mikko Hilpinen
  * @since 10.5.2020, v2
  * @param languageId Id of the language these descriptions are written in
  * @param descriptions Text-descriptions of various types (description role -> description text)
  */
case class NewDescription(languageId: Int, descriptions: Map[DescriptionRole, String])
