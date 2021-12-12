package utopia.keep.database.model.settings

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.keep.database.factory.settings.SettingValueFactory
import utopia.keep.model.partial.settings.SettingValueData
import utopia.keep.model.stored.settings.SettingValue
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter

/**
  * Used for constructing SettingValueModel instances and for inserting SettingValues to the database
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
object SettingValueModel 
	extends DataInserter[SettingValueModel, SettingValue, SettingValueData] 
		with DeprecatableAfter[SettingValueModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains SettingValue fieldId
	  */
	val fieldIdAttName = "fieldId"
	
	/**
	  * Name of the property that contains SettingValue value
	  */
	val valueAttName = "value"
	
	/**
	  * Name of the property that contains SettingValue created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains SettingValue deprecatedAfter
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains SettingValue fieldId
	  */
	def fieldIdColumn = table(fieldIdAttName)
	
	/**
	  * Column that contains SettingValue value
	  */
	def valueColumn = table(valueAttName)
	
	/**
	  * Column that contains SettingValue created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains SettingValue deprecatedAfter
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = SettingValueFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: SettingValueData) = 
		apply(None, Some(data.fieldId), data.value, Some(data.created), data.deprecatedAfter)
	
	override def complete(id: Value, data: SettingValueData) = SettingValue(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this value was specified
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when this value was replaced with another
	  * @return A model containing only the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param fieldId Id of the field this value is for
	  * @return A model containing only the specified fieldId
	  */
	def withFieldId(fieldId: Int) = apply(fieldId = Some(fieldId))
	
	/**
	  * @param id A SettingValue id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param value Value assigned for this field
	  * @return A model containing only the specified value
	  */
	def withValue(value: Value) = apply(value = Some(value))
}

/**
  * Used for interacting with SettingValues in the database
  * @param id SettingValue database id
  * @param fieldId Id of the field this value is for
  * @param value Value assigned for this field
  * @param created Time when this value was specified
  * @param deprecatedAfter Time when this value was replaced with another
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class SettingValueModel(id: Option[Int] = None, fieldId: Option[Int] = None, value: Value = Value.empty, 
	created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None) 
	extends StorableWithFactory[SettingValue]
{
	// IMPLEMENTED	--------------------
	
	override def factory = SettingValueModel.factory
	
	override def valueProperties = {
		import SettingValueModel._
		Vector("id" -> id, fieldIdAttName -> fieldId, valueAttName -> value, createdAttName -> created, 
			deprecatedAfterAttName -> deprecatedAfter)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter A new deprecatedAfter
	  * @return A new copy of this model with the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param fieldId A new fieldId
	  * @return A new copy of this model with the specified fieldId
	  */
	def withFieldId(fieldId: Int) = copy(fieldId = Some(fieldId))
	
	/**
	  * @param value A new value
	  * @return A new copy of this model with the specified value
	  */
	def withValue(value: Value) = copy(value = Some(value))
}

