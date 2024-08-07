package utopia.metropolis.model.post

import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType, VectorType}
import utopia.flow.collection.CollectionExtensions._

object NewDescription extends FromModelFactory[NewDescription]
{
	// ATTRIBUTES	----------------------------
	
	private val descriptionSchema = ModelDeclaration("role_id" -> IntType, "text" -> StringType)
	private val schema = ModelDeclaration("language_id" -> IntType, "descriptions" -> VectorType)
	
	
	// IMPLEMENTED	----------------------------
	
	override def apply(model: ModelLike[Property]) = schema.validate(model).flatMap { valid =>
		val languageId = valid("language_id").getInt
		// All specified descriptions must adhere to description schema
		valid("descriptions").getVector.tryMap { dv => descriptionSchema.validate(dv.getModel) }.map { descriptionModels =>
			// NB: If there are multiple descriptions for a single role, only one of those is preserved
			NewDescription(languageId, descriptionModels.map { dm => dm("role_id").getInt -> dm("text").getString }.toMap)
		}
	}
}

/**
  * Used for posting new / updated descriptions for organizations, devices, etc.
  * @author Mikko Hilpinen
  * @since 10.5.2020, v1
  * @param languageId Id of the language these descriptions are written in
  * @param descriptions Text-descriptions of various types (description role id -> description text)
  */
case class NewDescription(languageId: Int, descriptions: Map[Int, String])
