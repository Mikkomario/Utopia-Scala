package utopia.metropolis.model.partial.user

import java.time.Instant
import utopia.flow.generic.model.immutable.ModelDeclaration
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.mutable.DataType.{IntType, StringType}
import utopia.flow.time.Now
import utopia.metropolis.model.StyledModelConvertible

object UserSettingsData extends FromModelFactoryWithSchema[UserSettingsData]
{
	override def schema = ModelDeclaration("user_id" -> IntType, "name" -> StringType)
	
	override protected def fromValidatedModel(model: Model) =
		UserSettingsData(model("user_id").getInt, model("name").getString, model("email").string,
			model("created").getInstant)
}

/**
  * Versioned user-specific settings
  * @param userId Id of the described user
  * @param name Name used by this user
  * @param email Email address of this user
  * @param created Time when this UserSettings was first created
  * @param deprecatedAfter Time when these settings were replaced with a more recent version (if applicable)
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserSettingsData(userId: Int, name: String, email: Option[String] = None, created: Instant = Now, 
	deprecatedAfter: Option[Instant] = None) 
	extends StyledModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * @return Whether this data contains an email address
	  */
	def specifiesEmail = email.nonEmpty
	
	/**
	  * Whether this UserSettings has already been deprecated
	  */
	def isDeprecated = deprecatedAfter.isDefined
	/**
	  * Whether this UserSettings is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toSimpleModel = Model(Vector("user_id" -> userId, "name" -> name,
		"email" -> email, "last_updated" -> created))
	
	override def toModel =
		Model(Vector("user_id" -> userId, "name" -> name, "email" -> email, "created" -> created, 
			"deprecated_after" -> deprecatedAfter))
}

