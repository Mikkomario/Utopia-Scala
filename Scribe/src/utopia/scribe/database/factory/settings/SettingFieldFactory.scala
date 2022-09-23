package utopia.scribe.database.factory.settings

import utopia.flow.collection.value.typeless.Model
import utopia.scribe.database.ScribeTables
import utopia.scribe.model.partial.settings.SettingFieldData
import utopia.scribe.model.stored.settings.SettingField
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading SettingField data from the DB
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object SettingFieldFactory 
	extends FromValidatedRowModelFactory[SettingField] with FromRowFactoryWithTimestamps[SettingField]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = ScribeTables.settingField
	
	override def fromValidatedModel(valid: Model) = 
		SettingField(valid("id").getInt, SettingFieldData(valid("category").getString, 
			valid("name").getString, valid("description").string, valid("created").getInstant))
}

