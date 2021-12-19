package utopia.scribe.model.stored.settings

import utopia.scribe.database.access.single.settings.DbSingleSettingField
import utopia.scribe.model.partial.settings.SettingFieldData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a SettingField that has already been stored in the database
  * @param id id of this SettingField in the database
  * @param data Wrapped SettingField data
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class SettingField(id: Int, data: SettingFieldData) extends StoredModelConvertible[SettingFieldData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this SettingField in the database
	  */
	def access = DbSingleSettingField(id)
}

