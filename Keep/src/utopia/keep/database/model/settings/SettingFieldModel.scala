package utopia.keep.database.model.settings

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.keep.database.factory.settings.SettingFieldFactory
import utopia.keep.model.partial.settings.SettingFieldData
import utopia.keep.model.stored.settings.SettingField
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing SettingFieldModel instances and for inserting SettingFields to the database
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object SettingFieldModel extends DataInserter[SettingFieldModel, SettingField, SettingFieldData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains SettingField category
	  */
	val categoryAttName = "category"
	
	/**
	  * Name of the property that contains SettingField name
	  */
	val nameAttName = "name"
	
	/**
	  * Name of the property that contains SettingField description
	  */
	val descriptionAttName = "description"
	
	/**
	  * Name of the property that contains SettingField created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains SettingField category
	  */
	def categoryColumn = table(categoryAttName)
	
	/**
	  * Column that contains SettingField name
	  */
	def nameColumn = table(nameAttName)
	
	/**
	  * Column that contains SettingField description
	  */
	def descriptionColumn = table(descriptionAttName)
	
	/**
	  * Column that contains SettingField created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = SettingFieldFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: SettingFieldData) = 
		apply(None, Some(data.category), Some(data.name), data.description, Some(data.created))
	
	override def complete(id: Value, data: SettingFieldData) = SettingField(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param category Name of the broader category where this field belongs
	  * @return A model containing only the specified category
	  */
	def withCategory(category: String) = apply(category = Some(category))
	
	/**
	  * @param created Time when this field was introduced
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @return A model containing only the specified description
	  */
	def withDescription(description: String) = apply(description = Some(description))
	
	/**
	  * @param id A SettingField id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @return A model containing only the specified name
	  */
	def withName(name: String) = apply(name = Some(name))
}

/**
  * Used for interacting with SettingFields in the database
  * @param id SettingField database id
  * @param category Name of the broader category where this field belongs
  * @param created Time when this field was introduced
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class SettingFieldModel(id: Option[Int] = None, category: Option[String] = None, 
	name: Option[String] = None, description: Option[String] = None, created: Option[Instant] = None) 
	extends StorableWithFactory[SettingField]
{
	// IMPLEMENTED	--------------------
	
	override def factory = SettingFieldModel.factory
	
	override def valueProperties = {
		import SettingFieldModel._
		Vector("id" -> id, categoryAttName -> category, nameAttName -> name, 
			descriptionAttName -> description, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param category A new category
	  * @return A new copy of this model with the specified category
	  */
	def withCategory(category: String) = copy(category = Some(category))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param description A new description
	  * @return A new copy of this model with the specified description
	  */
	def withDescription(description: String) = copy(description = Some(description))
	
	/**
	  * @param name A new name
	  * @return A new copy of this model with the specified name
	  */
	def withName(name: String) = copy(name = Some(name))
}

