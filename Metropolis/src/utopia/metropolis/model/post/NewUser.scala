package utopia.metropolis.model.post

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ModelType, StringType, VectorType}
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.error.IllegalPostModelException
import utopia.metropolis.model.partial.user.UserSettingsData

import scala.util.{Failure, Success}

object NewUser extends FromModelFactory[NewUser]
{
	private val schema = ModelDeclaration("settings" -> ModelType, "password" -> StringType,
		"languages" -> VectorType)
	
	override def apply(model: template.Model[Property]) = schema.validate(model).toTry.flatMap { valid =>
		UserSettingsData(valid("settings").getModel).flatMap { settings =>
			// Languages must be parseable
			valid("languages").getVector.tryMap { v => NewLanguageProficiency(v.getModel) }.flatMap { languages =>
				// Either device id or new device data must be provided
				val deviceData = valid("device_id").int match
				{
					case Some(deviceId) => Success(Right(deviceId))
					case None =>
						valid("device").model match
						{
							case Some(deviceModel) => NewDevice(deviceModel).map { Left(_) }
							case None => Failure(
								new IllegalPostModelException("Either device_id or device must be provided"))
						}
				}
				deviceData.map { deviceData => NewUser(settings, valid("password").getString, languages, deviceData) }
			}
		}
	}
}

/**
  * A model used when creating new users from client side
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param settings Initial user settings
  * @param password Initial user password
  * @param languages List of the languages known by the user (with levels of familiarity)
  * @param device Either Right: Existing device id or Left: Device name + language id
  */
case class NewUser(settings: UserSettingsData, password: String, languages: Vector[NewLanguageProficiency],
				   device: Either[NewDevice, Int]) extends ModelConvertible
{
	override def toModel =
	{
		val deviceData: (String, Value) = device match
		{
			case Right(deviceId) => "device_id" -> deviceId
			case Left(newDevice) => "device" -> newDevice.toModel
		}
		Model(Vector("settings" -> settings.toModel, "password" -> password, "languages" -> languages.map { _.toModel },
			deviceData))
	}
}
