package utopia.metropolis.model.partial.user

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.IntType
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._

import scala.util.Try

object UserLanguageData
{
	private val schema = ModelDeclaration("language_id" -> IntType, "familiarity_id" -> IntType)
	
	/**
	  * Parses user language data from model. Doesn't expect model to contain a user id
	  * @param userId Id of the associated user
	  * @param model Model being parsed
	  * @return Parsed user language data. Failure if some properties were missing or invalid
	  */
	def apply(userId: Int, model: template.Model[Property]): Try[UserLanguageData] = schema.validate(model).toTry.map { valid =>
		UserLanguageData(userId, valid("language_id"), valid("familiarity_id"))
	}
}

/**
  * Basic data for linking a user with a language
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  * @param userId Id of the described user
  * @param languageId Linked language's id
  * @param familiarityId Id of the level of familiarity the user has using the specified language
  */
case class UserLanguageData(userId: Int, languageId: Int, familiarityId: Int)
{
	/**
	  * @return This link as a model without including user's id
	  */
	def toModelWithoutUser = Model(Vector("language_id" -> languageId,
		"familiarity_id" -> familiarityId))
}
