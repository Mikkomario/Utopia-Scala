package utopia.scribe.database.access.single.settings

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.scribe.database.factory.settings.SettingValueFactory
import utopia.scribe.database.model.settings.SettingValueModel
import utopia.scribe.model.stored.settings.SettingValue
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct SettingValues.
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait UniqueSettingValueAccess 
	extends SingleRowModelAccess[SettingValue] 
		with DistinctModelAccess[SettingValue, Option[SettingValue], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the field this value is for. None if no instance (or value) was found.
	  */
	def fieldId(implicit connection: Connection) = pullColumn(model.fieldIdColumn).int
	
	/**
	  * Value assigned for this field. None if no instance (or value) was found.
	  */
	def value(implicit connection: Connection) = pullColumn(model.valueColumn)
	
	/**
	  * Time when this value was specified. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Time when this value was replaced with another. None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SettingValueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SettingValueFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted SettingValue instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Deprecates all accessible SettingValues
	  * @return Whether any row was targeted
	  */
	def deprecate()(implicit connection: Connection) = deprecatedAfter = Now
	
	/**
	  * Updates the deprecatedAfter of the targeted SettingValue instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the fieldId of the targeted SettingValue instance(s)
	  * @param newFieldId A new fieldId to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def fieldId_=(newFieldId: Int)(implicit connection: Connection) = putColumn(model.fieldIdColumn, 
		newFieldId)
	
	/**
	  * Updates the value of the targeted SettingValue instance(s)
	  * @param newValue A new value to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def value_=(newValue: Value)(implicit connection: Connection) = putColumn(model.valueColumn, newValue)
}

