package utopia.metropolis.model.partial.description

import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

object DescriptionData extends FromModelFactoryWithSchema[DescriptionData]
{
	// ATTRIBUTES	------------------------------
	
	val schema = ModelDeclaration("role_id" -> IntType, "language_id" -> IntType, "text" -> StringType)
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def fromValidatedModel(model: Model[Constant]) = DescriptionData(model("role_id"), model("text"),
		model("language_id"), model("author_id"))
}

/**
  * Contains basic data about a description
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param roleId Id of this description's role
  * @param languageId Id of the language used in this description
  * @param text Description text
  * @param authorId Id of the user who wrote this description (optional)
  */
case class DescriptionData(roleId: Int, languageId: Int, text: String, authorId: Option[Int] = None)
	extends ModelConvertible
{
	override def toModel = Model("role_id" -> roleId, "text" -> text, "language_id" -> languageId,
		"author_id" -> authorId)
}
