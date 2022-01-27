package utopia.scribe.database.access.single.settings

import utopia.scribe.database.factory.settings.SettingFieldFactory
import utopia.scribe.database.model.settings.SettingFieldModel
import utopia.scribe.model.stored.settings.SettingField
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual SettingFields
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object DbSettingField extends SingleRowModelAccess[SettingField] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SettingFieldModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SettingFieldFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted SettingField instance
	  * @return An access point to that SettingField
	  */
	def apply(id: Int) = DbSingleSettingField(id)
	
	/**
	  * @param category Targeted setting category
	  * @return An access point to individual settings within that category
	  */
	def apply(category: String) = new DbCategorySettingField(category)
	
	
	// NESTED   -------------------
	
	class DbCategorySettingField(category: String) extends SingleRowModelAccess[SettingField] with SubView
	{
		// ATTRIBUTES   -----------
		
		override lazy val filterCondition = model.withCategory(category).toCondition
		
		
		// IMPLEMENTED  -----------
		
		override protected def parent = DbSettingField
		override def factory = parent.factory
		
		
		// OTHER    ---------------
		
		/**
		  * @param fieldName Name of the targeted field
		  * @return An access point to that field
		  */
		def apply(fieldName: String) = new DbNamedSettingField(fieldName)
		
		
		// NESTED   ---------------
		
		class DbNamedSettingField(fieldName: String) extends UniqueSettingFieldAccess with SubView
		{
			// IMPLEMENTED  -------
			
			override protected def parent = DbCategorySettingField.this
			override def filterCondition = model.withName(fieldName).toCondition
		}
	}
}

