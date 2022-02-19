package utopia.metropolis.model.post

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, StringType, VectorType}
import utopia.flow.util.CollectionExtensions._

import scala.util.Success

object NewUser extends FromModelFactory[NewUser]
{
	private val schema = ModelDeclaration("name" -> StringType, "password" -> StringType, "languages" -> VectorType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		// Languages must be parseable
		valid("languages").getVector.tryMap { v => NewLanguageProficiency(v.getModel) }.flatMap { languages =>
			val deviceData = valid("device_id").int match
			{
				case Some(deviceId) => Success(Some(Right(deviceId)))
				case None =>
					valid("device").model match
					{
						case Some(deviceModel) => NewDevice(deviceModel).map { d => Some(Left(d)) }
						case None => Success(None)
					}
			}
			deviceData.map { deviceData =>
				NewUser(valid("name").getString, valid("password").getString, languages, deviceData,
					valid("email").string, valid("request_refresh_token", "remember_me").getBoolean)
			}
		}
	}
}

/**
  * A model used when creating new users from client side
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param name User name
  * @param password Initial user password
  * @param languages List of the languages known by the user (with levels of familiarity)
  * @param device Either Right: Existing device id or Left: Device name + language id (optional).
  *               If None, this user is not connected with any device initially.
  * @param email Email address to assign for this user
  * @param requestRefreshToken Whether a refresh token should be generated and returned upon user creation
  *                            (default = false)
  */
case class NewUser(name: String, password: String, languages: Vector[NewLanguageProficiency],
                   device: Option[Either[NewDevice, Int]] = None, email: Option[String] = None,
                   requestRefreshToken: Boolean = false)
	extends ModelConvertible
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether email has been specified in this user data
	  */
	def specifiesEmail = email.isDefined
	
	
	// IMPLEMENTED	-------------------------
	
	override def toModel =
	{
		val deviceData = device.map
		{
			case Right(deviceId) => "device_id" -> (deviceId: Value)
			case Left(newDevice) => "device" -> (newDevice.toModel: Value)
		}
		Model(Vector[(String, Value)]("name" -> name, "email" -> email, "password" -> password,
			"languages" -> languages.map { _.toModel }, "request_refresh_token" -> requestRefreshToken) ++ deviceData)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param email An email address
	  * @return A copy of this model with that email address
	  */
	def withEmailAddress(email: String) = copy(email = Some(email))
}
