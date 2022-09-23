package utopia.scribe.database.access.single.settings

import utopia.flow.collection.value.typeless.Value

import java.time.Instant
import utopia.flow.generic.ValueConversions._
import utopia.scribe.database.factory.settings.SettingFieldFactory
import utopia.scribe.database.model.settings.SettingFieldModel
import utopia.scribe.model.stored.settings.SettingField
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct SettingFields.
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait UniqueSettingFieldAccess 
	extends SingleRowModelAccess[SettingField] 
		with DistinctModelAccess[SettingField, Option[SettingField], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Name of the broader category where this field belongs. None if no instance (or value) was found.
	  */
	def category(implicit connection: Connection) = pullColumn(model.categoryColumn).string
	
	/**
	  * The name of this instance. None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	
	/**
	  * The description of this instance. None if no instance (or value) was found.
	  */
	def description(implicit connection: Connection) = pullColumn(model.descriptionColumn).string
	
	/**
	  * Time when this field was introduced. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SettingFieldModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SettingFieldFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the category of the targeted SettingField instance(s)
	  * @param newCategory A new category to assign
	  * @return Whether any SettingField instance was affected
	  */
	def category_=(newCategory: String)(implicit connection: Connection) = 
		putColumn(model.categoryColumn, newCategory)
	
	/**
	  * Updates the created of the targeted SettingField instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any SettingField instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the description of the targeted SettingField instance(s)
	  * @param newDescription A new description to assign
	  * @return Whether any SettingField instance was affected
	  */
	def description_=(newDescription: String)(implicit connection: Connection) = 
		putColumn(model.descriptionColumn, newDescription)
	
	/**
	  * Updates the name of the targeted SettingField instance(s)
	  * @param newName A new name to assign
	  * @return Whether any SettingField instance was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
}

