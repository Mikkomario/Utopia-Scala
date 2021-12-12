package utopia.keep.model.stored.settings

import utopia.keep.database.access.single.settings.DbSingleSettingValue
import utopia.keep.model.partial.settings.SettingValueData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a SettingValue that has already been stored in the database
  * @param id id of this SettingValue in the database
  * @param data Wrapped SettingValue data
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class SettingValue(id: Int, data: SettingValueData) extends StoredModelConvertible[SettingValueData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this SettingValue in the database
	  */
	def access = DbSingleSettingValue(id)
}

