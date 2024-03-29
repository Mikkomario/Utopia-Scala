package utopia.metropolis.model.post

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.metropolis.model.error.IllegalPostModelException
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.util.MetropolisRegex

import scala.util.{Failure, Success}

object UserSettingsUpdate extends FromModelFactory[UserSettingsUpdate]
{
	// IMPLEMENTED  -------------------------
	
	override def apply(model: ModelLike[Property]) = {
		// Email address must be valid (if specified)
		val emailAddress = model("email").string
		if (emailAddress.forall { MetropolisRegex.email(_) })
			Success(UserSettingsUpdate(model("name"), emailAddress))
		else
			Failure(new IllegalPostModelException(s"'${emailAddress.get}' is not a valid email address"))
	}
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
