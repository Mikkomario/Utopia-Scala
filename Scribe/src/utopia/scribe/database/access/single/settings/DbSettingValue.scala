package utopia.scribe.database.access.single.settings

import utopia.scribe.database.factory.settings.SettingValueFactory
import utopia.scribe.database.model.settings.SettingValueModel
import utopia.scribe.model.stored.settings.SettingValue
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual SettingValues
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbSettingValue 
	extends SingleRowModelAccess[SettingValue] with NonDeprecatedView[SettingValue] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SettingValueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SettingValueFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted SettingValue instance
	  * @return An access point to that SettingValue
	  */
	def apply(id: Int) = DbSingleSettingValue(id)
	
	/**
	  * @param fieldId Id of the targeted field
	  * @return An access point to that field's latest value assignment
	  */
	def ofFieldWithId(fieldId: Int) = new DbSpecificSettingValue(fieldId)
	
	
	// NESTED   -------------------
	
	class DbSpecificSettingValue(fieldId: Int) extends UniqueSettingValueAccess with SubView
	{
		// IMPLEMENTED  -----------
		
		override protected def parent = DbSettingValue
		override def filterCondition = model.withFieldId(fieldId).toCondition
	}
}

