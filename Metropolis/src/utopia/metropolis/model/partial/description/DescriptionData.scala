package utopia.metropolis.model.partial.description

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, IntType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.enumeration.DescriptionRole

object DescriptionData extends FromModelFactory[DescriptionData]
{
	// ATTRIBUTES	------------------------------
	
	private val schema = ModelDeclaration("role" -> IntType, "language_id" -> IntType, "text" -> StringType)
	
	
	// IMPLEMENTED	------------------------------
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		DescriptionRole.forId(valid("role")).map { role =>
			DescriptionData(role, valid("language_id"), valid("text"), valid("author_id"))
		}
	}
}

/**
  * Contains basic data about a description
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param role Role of this description
  * @param languageId Id of the language used in this description
  * @param text Description text
  * @param authorId Id of the user who wrote this description (optional)
  */
case class DescriptionData(role: DescriptionRole, languageId: Int, text: String, authorId: Option[Int] = None)
	extends ModelConvertible
{
	override def toModel = Model("role" -> role.id, "text" -> text, "language_id" -> languageId,
		"author_id" -> authorId)
}
