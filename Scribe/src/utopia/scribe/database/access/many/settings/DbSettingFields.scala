package utopia.scribe.database.access.many.settings

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple SettingFields at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbSettingFields extends ManySettingFieldsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted SettingFields
	  * @return An access point to SettingFields with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbSettingFieldsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbSettingFieldsSubset(targetIds: Set[Int]) extends ManySettingFieldsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

