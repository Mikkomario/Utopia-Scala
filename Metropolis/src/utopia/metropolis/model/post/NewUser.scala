package utopia.metropolis.model.post

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, ModelValidationFailedException, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, StringType, VectorType}
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.partial.user.UserSettingsData

import scala.util.Success

object NewUser extends FromModelFactory[NewUser]
{
	private val schema = ModelDeclaration("password" -> StringType, "languages" -> VectorType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		// Languages must be parseable
		valid("languages").getVector.tryMap { v => NewLanguageProficiency(v.getModel) }.flatMap { languages =>
			// Either name of settings must be specified
			val settingsData = valid("settings").model match
			{
				case Some(settingsModel) => UserSettingsData(settingsModel).map { Right(_) }
				case None =>
					valid("name").string.filterNot { _.isEmpty }.toTry { new ModelValidationFailedException(
						"Either 'name' (string) or 'settings' (object) must be specified") }.map { Left(_) }
			}
			settingsData.flatMap { settingsData =>
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
					NewUser(settingsData, valid("password").getString, languages, deviceData,
						valid("remember_me").getBoolean)
				}
			}
		}
	}
}

/**
  * A model used when creating new users from client side
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param settingsData Either Right) User name and email or Left) User name only
  * @param password Initial user password
  * @param languages List of the languages known by the user (with levels of familiarity)
  * @param device Either Right: Existing device id or Left: Device name + language id (optional).
  *               If None, this user is not connected with any device initially.
  * @param rememberOnDevice Whether this user should receive a device key for this device, allowing future
  *                         logins to be made automatically (default = false). Ignored if no device data is passed.
  */
case class NewUser(settingsData: Either[String, UserSettingsData], password: String,
				   languages: Vector[NewLanguageProficiency], device: Option[Either[NewDevice, Int]] = None,
				   rememberOnDevice: Boolean = false)
	extends ModelConvertible
{
	// COMPUTED	-----------------------------
	
	/**
	  * @return Whether email has been specified in this user data
	  */
	def specifiesEmail = settingsData.exists { _.specifiesEmail }
	
	/**
	  * @return Proposed username
	  */
	def userName = settingsData match
	{
		case Left(name) => name
		case Right(settings) => settings.name
	}
	
	/**
	  * @return Proposed email address. None if not specified.
	  */
	def email = settingsData.rightOption.flatMap { _.email }
	
	
	// IMPLEMENTED	-------------------------
	
	override def toModel =
	{
		val deviceData = device.map
		{
			case Right(deviceId) => "device_id" -> (deviceId: Value)
			case Left(newDevice) => "device" -> (newDevice.toModel: Value)
		}
		val settingsData: (String, Value) = this.settingsData match
		{
			case Right(settings) => "settings" -> settings.toModel
			case Left(name) => "name" -> name
		}
		Model(Vector[(String, Value)](settingsData, "password" -> password, "languages" -> languages.map { _.toModel },
			"remember_me" -> rememberOnDevice) ++ deviceData)
	}
}
