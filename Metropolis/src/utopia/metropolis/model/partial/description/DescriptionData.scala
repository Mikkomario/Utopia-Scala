package utopia.metropolis.model.partial.description

import java.time.Instant
import utopia.flow.datastructure.immutable.{Constant, Model, ModelDeclaration}
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.Now

object DescriptionData extends FromModelFactoryWithSchema[DescriptionData]
{
	// ATTRIBUTES	------------------------------
	
	val schema = ModelDeclaration("role_id" -> IntType, "language_id" -> IntType, "text" -> StringType)
	
	
	// IMPLEMENTED	------------------------------
	
	override protected def fromValidatedModel(model: Model[Constant]) =
		DescriptionData(model("role_id"), model("text"), model("language_id"), model("author_id"), model("created"),
			model("deprecated_after"))
}

/**
  * Represents some description of some item in some language
  * @param roleId Id of the role of this description
  * @param languageId Id of the language this description is written in
  * @param text This description as text / written description
  * @param authorId Id of the user who wrote this description (if known and applicable)
  * @param created Time when this description was written
  * @param deprecatedAfter Time when this description was removed or replaced with a new version
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class DescriptionData(roleId: Int, languageId: Int, text: String, authorId: Option[Int] = None, 
	created: Instant = Now, deprecatedAfter: Option[Instant] = None) 
	extends ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this Description has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	
	/**
	  * Whether this Description is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("role_id" -> roleId, "language_id" -> languageId, "text" -> text, 
			"author_id" -> authorId, "created" -> created, "deprecated_after" -> deprecatedAfter))
}

