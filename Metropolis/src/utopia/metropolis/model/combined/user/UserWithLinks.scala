package utopia.metropolis.model.combined.user

import utopia.flow.collection.template.typeless
import utopia.flow.collection.template.typeless.Property
import utopia.flow.datastructure.immutable.ModelDeclaration
import utopia.flow.datastructure.template
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.{FromModelFactory, IntType, ModelType}
import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.stored.user.{UserLanguageLink, UserSettings}

@deprecated("Device classes will be removed. Please use DetailedUser instead.", "v2.1")
object UserWithLinks extends FromModelFactory[UserWithLinks]
{
	private val schema = ModelDeclaration("id" -> IntType, "settings" -> ModelType)
	
	override def apply(model: typeless.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		UserSettings(valid("settings").getModel).map { settings =>
			UserWithLinks(settings,
				valid("language_links").getVector.flatMap { _.model }.flatMap { UserLanguageLink(_).toOption },
				valid("device_ids").getVector.flatMap { _.int })
		}
	}
}

/**
  * This user model contains links to known languages and used devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param settings User settings
  * @param languages Languages known to the user, with proficiency levels
  * @param deviceIds Ids of the devices known to the user
  */
@deprecated("Device classes will be removed. Please use DetailedUser instead.", "v2.1")
case class UserWithLinks(settings: UserSettings, languages: Vector[UserLanguageLink], deviceIds: Vector[Int])
	extends StyledModelConvertible
{
	// COMPUTED --------------------------
	
	/**
	  * @return Id of this user
	  */
	def id = settings.userId
	
	
	// IMPLEMENTED  ----------------------
	
	override def toModel =
		Model(Vector("id" -> id, "settings" -> settings.toModel, "language_links" -> languages.map { _.toModel },
			"device_ids" -> deviceIds))
	
	override def toSimpleModel = Model(Vector("id" -> id, "language_links" -> languages.map { _.toSimpleModel },
		"device_ids" -> deviceIds)) ++ (settings.toSimpleModel - "user_id")
}
