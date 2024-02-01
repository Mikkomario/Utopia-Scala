package utopia.metropolis.model.stored.user

import utopia.flow.generic.model.immutable.{ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.{ModelLike, Property}
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StyledStoredModelConvertible}

import scala.util.Try

object UserLanguageLink extends StoredFromModelFactory[UserLanguageLink, UserLanguageLinkData]
{
	// ATTRIBUTES   -----------------------------
	
	private val idSchema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	
	// IMPLEMENTED  -----------------------------
	
	override def dataFactory = UserLanguageLinkData
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Parses a user language from model. Doesn't expect to find user id from the model
	  * @param userId Id of associated user
	  * @param model Model being parsed
	  * @return Parsed user language data. Failure if some properties were missing or invalid
	  */
	def apply(userId: Int, model: ModelLike[Property]): Try[UserLanguageLink] =
		idSchema.validate(model).flatMap { valid =>
			UserLanguageLinkData(userId, model).map { data => UserLanguageLink(valid("id").getInt, data) }
		}
}

/**
  * Represents a UserLanguage that has already been stored in the database
  * @param id id of this UserLanguage in the database
  * @param data Wrapped UserLanguage data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserLanguageLink(id: Int, data: UserLanguageLinkData)
	extends StyledStoredModelConvertible[UserLanguageLinkData]
{
	override protected def includeIdInSimpleModel = false
}
