package utopia.metropolis.model.partial.user

import java.time.Instant
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactoryWithSchema, IntType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

import scala.util.Try

object UserLanguageLinkData extends FromModelFactoryWithSchema[UserLanguageLinkData]
{
	// ATTRIBUTES   -----------------------------
	
	private val withoutUserSchema = ModelDeclaration("language_id" -> IntType, "familiarity_id" -> IntType)
	override val schema = withoutUserSchema + PropertyDeclaration("user_id", IntType)
	
	
	// IMPLEMENTED  -----------------------------
	
	override protected def fromValidatedModel(model: Model) =
		UserLanguageLinkData(model("user_id"), model("language_id"), model("familiarity_id"))
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Parses user language data from model. Doesn't expect model to contain a user id
	  * @param userId Id of the associated user
	  * @param model Model being parsed
	  * @return Parsed user language data. Failure if some properties were missing or invalid
	  */
	def apply(userId: Int, model: template.Model[Property]): Try[UserLanguageLinkData] =
		withoutUserSchema.validate(model).toTry.map { valid =>
			UserLanguageLinkData(userId, valid("language_id"), valid("familiarity_id"))
		}
}

/**
  * Links user with their language familiarity levels
  * @param userId Id of the user who's being described
  * @param languageId Id of the language known to the user
  * @param familiarityId Id of the user's familiarity level in the referenced language
  * @param created Time when this UserLanguage was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserLanguageLinkData(userId: Int, languageId: Int, familiarityId: Int, created: Instant = Now)
	extends StyledModelConvertible
{
	// COMPUTED ------------------------
	
	/**
	  * @return This link as a model without including user's id
	  */
	@deprecated("Please use toSimpleModel instead", "2.0")
	def toModelWithoutUser = toModel - "user_id"
	
	
	// IMPLEMENTED	--------------------
	
	override def toSimpleModel =
		Model(Vector("language_id" -> languageId, "familiarity_id" -> familiarityId))
	
	override def toModel =
		Model(Vector("user_id" -> userId, "language_id" -> languageId, "familiarity_id" -> familiarityId, 
			"created" -> created))
}

