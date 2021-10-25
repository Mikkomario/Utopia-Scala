package utopia.metropolis.model.post

import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, ModelValidationFailedException, PropertyDeclaration, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, StringType}
import utopia.flow.generic.ValueConversions._
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.util.CollectionExtensions._

import scala.util.Success

object PasswordChange extends FromModelFactory[PasswordChange]
{
	// ATTRIBUTES   --------------------------
	
	private val schema = ModelDeclaration(PropertyDeclaration("new_password", StringType))
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		val newPassword = valid("new_password").getString
		valid("token").string match
		{
			case Some(emailKey) => Success(emailAuthenticated(newPassword, emailKey, valid("device_id")))
			case None =>
				valid("old_password").string.filterNot { _.isEmpty }
					.toTry { new ModelValidationFailedException("Either 'token' or 'old_password' required") }
					.map { oldPassword => sessionAuthenticated(oldPassword, newPassword) }
		}
	}
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new password change model
	  * for situations where user is already logged in and wants to change their password
	  * @param oldPassword User's current password
	  * @param newPassword User's new password
	  * @return A new password change model
	  */
	def sessionAuthenticated(oldPassword: String, newPassword: String) =
		apply(newPassword, Right(oldPassword))
	
	/**
	  * Creates a new password change model for situations where user has forgot their password
	  * @param newPassword New password for the user
	  * @param emailToken A token the user received through a validation email
	  * @param deviceId Id of the device the user wants to log in to (optional)
	  * @return A new password change model
	  */
	def emailAuthenticated(newPassword: String, emailToken: String, deviceId: Option[Int] = None) =
		apply(newPassword, Left(emailToken -> deviceId))
}

/**
  * Used for posting password change requests to the server
  * @author Mikko Hilpinen
  * @since 3.12.2020, v1
  * @param newPassword New password to assign
  * @param authentication Either<br>
  *                       Right) User's current password (when user simply wants to change their password) or<br>
  *                       Left) An email validation key + optional device id (when user has forgotten their password)
  */
case class PasswordChange(newPassword: String, authentication: Either[(String, Option[Int]), String])
	extends ModelConvertible
{
	// COMPUTED ----------------------------
	
	/**
	  * @return An email based authentication (email validation key + optional device id). None if session-based
	  *         authentication (old password) is provided instead.
	  */
	def emailAuthentication = authentication.leftOption
	
	/**
	  * @return Previous password (None if authenticated via email)
	  */
	def oldPassword = authentication.rightOption
	
	/**
	  * @return A validation key received through email (None if authenticated via active session & old password)
	  */
	def emailValidationToken = emailAuthentication.map { _._1 }
	
	/**
	  * @return Id of device to log in to (None if unspecified). Only provided in password recovery process,
	  *         and is optional there also
	  */
	def deviceId = emailAuthentication.map { _._2 }
	
	override def toModel =
	{
		val authParams: Vector[(String, Value)] = authentication match
		{
			case Right(oldPassword) => Vector("old_password" -> oldPassword)
			case Left((emailKey, deviceId)) => Vector("token" -> emailKey, "device_id" -> deviceId)
		}
		Model(("new_password" -> (newPassword: Value)) +: authParams)
	}
}
