package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.{Model, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.partial.user.UserSettingsData

import scala.util.Success

object UserSettingsUpdate extends FromModelFactory[UserSettingsUpdate]
{
	// IMPLEMENTED  -------------------------
	
	override def apply(model: template.Model[Property]) =
	{
		val name = model("name").string
		Success(model("email_key").string match
		{
			case Some(key) => UserSettingsUpdate(name, Some(Right(key)))
			case None => UserSettingsUpdate(name, model("email").string.map { Left(_) })
		})
	}
}

/**
  * Used for posting complete or partial user settings updates
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  * @param name New user name (optional)
  * @param email Either Right) email validation code or Left) new email. Should be Right when email validation is
  *              enabled on the server side and Left if that feature is disabled. None if email shouldn't be affected.
  */
case class UserSettingsUpdate(name: Option[String] = None, email: Option[Either[String, String]] = None)
	extends ModelConvertible
{
	// COMPUTED	----------------------------
	
	/**
	  * @return Proposed email address. None if not specified or expecting email validation to be used.
	  */
	def newEmailAddress = email.flatMap { _.leftOption }
	
	/**
	  * @return Email validation key matching previously reserved email address. None if unspecified or when email
	  *         validation is not used.
	  */
	def emailValidationKey = email.flatMap { _.rightOption }
	
	/*
	/**
	  * @return Either Right) New user name and email validation key or Left) New user settings. Failure if some
	  *         of the data was missing.
	  */
	def toPost = name match
	{
		case Some(name) =>
			email match
			{
				case Some(email) =>
					Success(email.mapBoth { email => UserSettingsData(name, email) } { key => name -> key })
				case None => Failure(new IllegalPostModelException(
					"email must be specified via 'email_key' or 'email' property"))
			}
		case None => Failure(new IllegalPostModelException("Required property 'name' is missing"))
	}*/
	
	
	// IMPLEMENTED	------------------------
	
	override def toModel =
	{
		val emailProperty = email.map
		{
			case Right(key) => "email_key" -> (key: Value)
			case Left(email) => "email" -> (email: Value)
		}
		Model(Vector("name" -> (name: Value)) ++ emailProperty)
	}
	
	
	// OTHER	----------------------------
	
	/**
	  * @param existingData Existing user data
	  * @return Whether this user settings update is potentially different from the specified data
	  */
	def isPotentiallyDifferentFrom(existingData: UserSettingsData) = name.exists { _ != existingData.name } || email.exists
	{
		case Right(_) => true
		case Left(email) => !existingData.email.contains(email)
	}
	
	/**
	  * @param existingEmail Existing email address (if available)
	  * @return Whether this user settings update would potentially change / affect that email address
	  */
	def definesPotentiallyDifferentEmailThan(existingEmail: Option[String]) = email.exists
	{
		case Right(_) => true
		case Left(email) => !existingEmail.contains(email)
	}
	
	/*
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
	 */
}
