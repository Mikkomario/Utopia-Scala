package utopia.metropolis.model.stored.user

import utopia.flow.datastructure.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.IntType
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.Stored

import scala.util.Try

object UserLanguage
{
	private val idSchema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	/**
	  * Parses a user language from model. Doesn't expect to find user id from the model
	  * @param userId Id of associated user
	  * @param model Model being parsed
	  * @return Parsed user language data. Failure if some properties were missing or invalid
	  */
	def apply(userId: Int, model: template.Model[Property]): Try[UserLanguageLink] = idSchema.validate(model).toTry.flatMap { valid =>
		UserLanguageLinkData(userId, model).map { data =>
			UserLanguageLink(valid("id"), data)
		}
	}
}

/**
  * Used for linking users with their proficient languages
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
case class UserLanguage(id: Int, data: UserLanguageLinkData) extends Stored[UserLanguageLinkData]
{
	/**
	  * @return This link as a model without including user id
	  */
	def toModelWithoutUser = data.toModelWithoutUser + Constant("id", id)
}
