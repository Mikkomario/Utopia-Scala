package utopia.scribe.database.access.many.settings

import utopia.flow.generic.ValueConversions._
import utopia.scribe.model.stored.settings.SettingValue
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple SettingValues at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbSettingValues extends ManySettingValuesAccess with NonDeprecatedView[SettingValue]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted SettingValues
	  * @return An access point to SettingValues with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbSettingValuesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbSettingValuesSubset(targetIds: Set[Int]) extends ManySettingValuesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

