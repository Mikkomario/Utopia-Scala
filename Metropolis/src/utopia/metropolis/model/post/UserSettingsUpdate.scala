package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.IllegalPostModelException
import utopia.metropolis.model.partial.user.UserSettingsData

import scala.util.Success

object UserSettingsUpdate extends FromModelFactory[UserSettingsUpdate]
{
	
	
	override def apply(model: template.Model[Property]) =
		Success(UserSettingsUpdate(model("name"), model("email")))
}

/**
  * Used for posting complete or partial user settings updates
  * @author Mikko Hilpinen
  * @since 20.5.2020, v2
  */
case class UserSettingsUpdate(name: Option[String] = None, email: Option[String] = None) extends ModelConvertible
{
	// COMPUTED	----------------------------
	
	/**
	  * @return Completely new user settings data based on this update. Failure if this update doesn't contain all required
	  *         information.
	  */
	def toPost = require(email, "email").flatMap { email =>
		require(name, "name").map { name => UserSettingsData(name, email) }
	}
	
	
	// IMPLEMENTED	------------------------
	
	override def toModel = Model(Vector("name" -> name, "email" -> email))
	
	
	// OTHER	----------------------------
	
	/**
	  * @param existingData Existing user settings
	  * @return New user settings version based on the existing version + updates from this model
	  */
	def toPatch(existingData: UserSettingsData) = UserSettingsData(name.getOrElse(existingData.name),
		email.getOrElse(existingData.email))
	
	/**
	  * @param existingData Existing user settings
	  * @return Whether this update would change those settings
	  */
	def isDifferentFrom(existingData: UserSettingsData) = name.exists { _ != existingData.name } ||
		email.exists { _ != existingData.email }
	
	private def require[A](field: Option[A], fieldName: => String) = field.toTry {
		new IllegalPostModelException(s"Required property '$fieldName' is missing") }
}
