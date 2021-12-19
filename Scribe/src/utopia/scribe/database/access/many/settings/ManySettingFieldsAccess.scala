package utopia.scribe.database.access.many.settings

import java.time.Instant
import utopia.flow.generic.ValueConversions._
import utopia.scribe.database.factory.settings.SettingFieldFactory
import utopia.scribe.database.model.settings.SettingFieldModel
import utopia.scribe.model.stored.settings.SettingField
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManySettingFieldsAccess
{
	// NESTED	--------------------
	
	private class ManySettingFieldsSubView(override val parent: ManyRowModelAccess[SettingField], 
		override val filterCondition: Condition) 
		extends ManySettingFieldsAccess with SubView
}

/**
  * A common trait for access points which target multiple SettingFields at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait ManySettingFieldsAccess extends ManyRowModelAccess[SettingField] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * categorys of the accessible SettingFields
	  */
	def categorys(implicit connection: Connection) = pullColumn(model.categoryColumn).map { v => v.getString }
	
	/**
	  * names of the accessible SettingFields
	  */
	def names(implicit connection: Connection) = pullColumn(model.nameColumn).map { v => v.getString }
	
	/**
	  * descriptions of the accessible SettingFields
	  */
	def descriptions(implicit connection: Connection) = pullColumn(model.descriptionColumn)
		.flatMap { _.string }
	
	/**
	  * creationTimes of the accessible SettingFields
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SettingFieldModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SettingFieldFactory
	
	override protected def defaultOrdering = Some(factory.defaultOrdering)
	
	override def filter(additionalCondition: Condition): ManySettingFieldsAccess = 
		new ManySettingFieldsAccess.ManySettingFieldsSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the category of the targeted SettingField instance(s)
	  * @param newCategory A new category to assign
	  * @return Whether any SettingField instance was affected
	  */
	def categorys_=(newCategory: String)(implicit connection: Connection) = 
		putColumn(model.categoryColumn, newCategory)
	
	/**
	  * Updates the created of the targeted SettingField instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any SettingField instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the description of the targeted SettingField instance(s)
	  * @param newDescription A new description to assign
	  * @return Whether any SettingField instance was affected
	  */
	def descriptions_=(newDescription: String)(implicit connection: Connection) = 
		putColumn(model.descriptionColumn, newDescription)
	
	/**
	  * Updates the name of the targeted SettingField instance(s)
	  * @param newName A new name to assign
	  * @return Whether any SettingField instance was affected
	  */
	def names_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}

