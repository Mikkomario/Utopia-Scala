package utopia.metropolis.model.stored.user

import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StyledStoredModelConvertible}

object UserSettings extends StoredFromModelFactory[UserSettings, UserSettingsData]
{
	override def dataFactory = UserSettingsData
}

/**
  * Represents a UserSettings that has already been stored in the database
  * @param id id of this UserSettings in the database
  * @param data Wrapped UserSettings data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserSettings(id: Int, data: UserSettingsData) extends StyledStoredModelConvertible[UserSettingsData]
{
	override protected def includeIdInSimpleModel = true
}
