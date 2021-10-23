package utopia.metropolis.model.stored.user

import utopia.flow.datastructure.immutable.{Constant, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.IntType
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserLanguageData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StyledStoredModelConvertible}

import scala.util.Try

object UserLanguage extends StoredFromModelFactory[UserLanguage, UserLanguageData]
{
	// ATTRIBUTES   -----------------------------
	
	private val idSchema = ModelDeclaration(PropertyDeclaration("id", IntType))
	
	
	// IMPLEMENTED  -----------------------------
	
	override def dataFactory = UserLanguageData
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Parses a user language from model. Doesn't expect to find user id from the model
	  * @param userId Id of associated user
	  * @param model Model being parsed
	  * @return Parsed user language data. Failure if some properties were missing or invalid
	  */
	def apply(userId: Int, model: template.Model[Property]): Try[UserLanguage] =
		idSchema.validate(model).toTry.flatMap { valid =>
			UserLanguageData(userId, model).map { data => UserLanguage(valid("id").getInt, data) }
		}
}

/**
  * Represents a UserLanguage that has already been stored in the database
  * @param id id of this UserLanguage in the database
  * @param data Wrapped UserLanguage data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserLanguage(id: Int, data: UserLanguageData) extends StyledStoredModelConvertible[UserLanguageData]
{
	/**
	  * @return This link as a model without including user id
	  */
	@deprecated("Please use toSimpleModel instead", "v2.0")
	def toModelWithoutUser = data.toModelWithoutUser + Constant("id", id)
	
	override protected def includeIdInSimpleModel = false
}
