package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.Model
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{ ModelConvertible, SureFromModelFactory}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.user.UserSettingsData

object UserSettingsUpdate extends SureFromModelFactory[UserSettingsUpdate]
{
	// IMPLEMENTED  -------------------------
	
	override def parseFrom(model: template.Model[Property]) = UserSettingsUpdate(model("name"), model("email"))
}

/**
  * Used for posting complete or partial user settings updates
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  * @param name New user name (optional)
  * @param email New email address (optional)
  */
case class UserSettingsUpdate(name: Option[String] = None, email: Option[String] = None) extends ModelConvertible
{
	// IMPLEMENTED	------------------------
	
	override def toModel = Model(Vector("name" -> name, "email" -> email))
	
	
	// OTHER	----------------------------
	
	/**
	  * @param existingData Existing user data
	  * @return Whether this user settings update is different from the specified data
	  */
	def isDifferentFrom(existingData: UserSettingsData) =
		name.exists { _ != existingData.name } || definesDifferentEmailThan(existingData.email)
	
	/**
	  * @param existingEmail Existing email address (if available)
	  * @return Whether this user settings update would change / affect that email address
	  */
	def definesDifferentEmailThan(existingEmail: Option[String]) =
		email.exists { !existingEmail.contains(_) }
}
