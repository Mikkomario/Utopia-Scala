package utopia.metropolis.model.stored.user

import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.{FromModelFactory, ModelConvertible}
import utopia.metropolis.model.Extender

object User extends FromModelFactory[User]
{
	override def apply(model: Model[Property]) =
	{
		// Parses the settings and retrieves user id from those
		UserSettings(model).map { settings => User(settings.userId, settings) }
	}
}

/**
  * Represents a user registered in the database
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  * @param id This user's id in DB
  * @param settings This user's current settings
  */
case class User(id: Int, settings: UserSettings) extends Extender[UserSettings] with ModelConvertible
{
	override def wrapped = settings
	
	override def toModel = settings.toModel
}
