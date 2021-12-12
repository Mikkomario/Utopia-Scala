package utopia.keep.database.factory.settings

import utopia.keep.model.combined.settings.AssignedSetting
import utopia.keep.model.stored.settings.{SettingField, SettingValue}
import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading AssignedSettings from the database
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object AssignedSettingFactory 
	extends CombiningFactory[AssignedSetting, SettingField, SettingValue] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = SettingValueFactory
	
	override def nonDeprecatedCondition = childFactory.nonDeprecatedCondition
	
	override def parentFactory = SettingFieldFactory
	
	override def apply(field: SettingField, assignment: SettingValue) = AssignedSetting(field, assignment)
}

