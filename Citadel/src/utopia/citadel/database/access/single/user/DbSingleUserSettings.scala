package utopia.citadel.database.access.single.user

import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual UserSettings, based on their id
  * @since 2021-10-23
  */
case class DbSingleUserSettings(id: Int) 
	extends UniqueUserSettingsAccess with SingleIntIdModelAccess[UserSettings]

