package utopia.keep.database.access.single.settings

import utopia.keep.model.stored.settings.SettingValue
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual SettingValues, based on their id
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class DbSingleSettingValue(id: Int) 
	extends UniqueSettingValueAccess with SingleIntIdModelAccess[SettingValue]

