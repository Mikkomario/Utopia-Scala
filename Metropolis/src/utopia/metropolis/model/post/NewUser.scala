package utopia.metropolis.model.post

import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.immutable.{Model, ModelDeclaration, ModelValidationFailedException, Value}
import utopia.flow.datastructure.template
import utopia.flow.datastructure.template.Property
import utopia.flow.generic.{FromModelFactory, ModelConvertible, ModelType, StringType, VectorType}
import utopia.flow.util.CollectionExtensions._
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
				// Either device id or device name must be provided
				val deviceId = valid("device_id").int
				val deviceData = valid("device").getModel
				val deviceName = deviceData("name").string.filterNot { _.isEmpty }
				val languageId = deviceData("language_id").int
				if (deviceId.isEmpty && (deviceName.isEmpty || languageId.isEmpty))
					Failure(new ModelValidationFailedException("Either device_id or device with name and language_id must be provided"))
				else
				{
					val deviceIdOrName = deviceId match
					{
						case Some(id) => Right(id)
						case None => Left(deviceName.get -> languageId.get)
					}
					Success(NewUser(settings, valid("password").getString, languages, deviceIdOrName))
				}
			}
		}
	}
}

/**
  * A model used when creating new users from client side
  * @author Mikko Hilpinen
  * @since 2.5.2020, v2
  * @param settings Initial user settings
  * @param password Initial user password
  * @param languages List of the languages known by the user (with levels of familiarity)
  * @param device Either Right: Existing device id or Left: Device name + language id
  */
case class NewUser(settings: UserSettingsData, password: String, languages: Vector[NewLanguageProficiency],
				   device: Either[(String, Int), Int]) extends ModelConvertible
{
	override def toModel =
	{
		val deviceData: (String, Value) = device match
		{
			case Right(deviceId) => "device_id" -> deviceId
			case Left(deviceNameData) =>
				val deviceModel = Model(Vector("name" -> deviceNameData._1, "language_id" -> deviceNameData._2))
				"device" -> deviceModel
		}
		Model(Vector("settings" -> settings.toModel, "password" -> password, "languages" -> languages.map { _.toModel },
			deviceData))
	}
}
