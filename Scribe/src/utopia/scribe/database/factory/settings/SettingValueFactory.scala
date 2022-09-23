package utopia.scribe.database.factory.settings

import utopia.flow.collection.value.typeless.Model
import utopia.scribe.database.ScribeTables
import utopia.scribe.database.model.settings.SettingValueModel
import utopia.scribe.model.partial.settings.SettingValueData
import utopia.scribe.model.stored.settings.SettingValue
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading SettingValue data from the DB
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object SettingValueFactory 
	extends FromValidatedRowModelFactory[SettingValue] with FromRowFactoryWithTimestamps[SettingValue] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = SettingValueModel.nonDeprecatedCondition
	
	override def table = ScribeTables.settingValue
	
	override def fromValidatedModel(valid: Model) = 
		SettingValue(valid("id").getInt, SettingValueData(valid("fieldId").getInt, valid("value"), 
			valid("created").getInstant, valid("deprecatedAfter").instant))
}

