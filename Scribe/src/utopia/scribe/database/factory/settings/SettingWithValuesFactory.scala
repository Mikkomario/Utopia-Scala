package utopia.scribe.database.factory.settings

import utopia.scribe.model.combined.settings.SettingWithValues
import utopia.scribe.model.stored.settings.{SettingField, SettingValue}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading SettingWithValuess from the database
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object SettingWithValuesFactory 
	extends MultiCombiningFactory[SettingWithValues, SettingField, SettingValue] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = SettingValueFactory
	
	override def isAlwaysLinked = false
	
	override def nonDeprecatedCondition = childFactory.nonDeprecatedCondition
	
	override def parentFactory = SettingFieldFactory
	
	override def apply(field: SettingField, assignments: Vector[SettingValue]) =
		SettingWithValues(field, assignments)
}

