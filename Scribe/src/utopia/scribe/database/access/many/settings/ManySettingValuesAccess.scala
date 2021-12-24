package utopia.scribe.database.access.many.settings

import java.time.Instant
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.scribe.database.factory.settings.SettingValueFactory
import utopia.scribe.database.model.settings.SettingValueModel
import utopia.scribe.model.stored.settings.SettingValue
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManySettingValuesAccess
{
	// NESTED	--------------------
	
	private class ManySettingValuesSubView(override val parent: ManyRowModelAccess[SettingValue], 
		override val filterCondition: Condition) 
		extends ManySettingValuesAccess with SubView
}

/**
  * A common trait for access points which target multiple SettingValues at a time
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
trait ManySettingValuesAccess
	extends ManyRowModelAccess[SettingValue] with Indexed with FilterableView[ManySettingValuesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * fieldIds of the accessible SettingValues
	  */
	def fieldIds(implicit connection: Connection) = pullColumn(model.fieldIdColumn).map { v => v.getInt }
	
	/**
	  * values of the accessible SettingValues
	  */
	def values(implicit connection: Connection) = pullColumn(model.valueColumn)
	
	/**
	  * creationTimes of the accessible SettingValues
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn).map { _.getInstant }
	
	/**
	  * deprecationTimes of the accessible SettingValues
	  */
	def deprecationTimes(implicit connection: Connection) = 
		pullColumn(model.deprecatedAfterColumn).flatMap { _.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = SettingValueModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = SettingValueFactory
	
	override def filter(additionalCondition: Condition): ManySettingValuesAccess = 
		new ManySettingValuesAccess.ManySettingValuesSubView(this, additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * @param fieldId Id of the targeted setting field
	  * @return An access point to that field's value assignments
	  */
	def ofFieldWithId(fieldId: Int) = filter(model.withFieldId(fieldId).toCondition)
	
	/**
	  * Updates the created of the targeted SettingValue instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Deprecates all accessible SettingValues
	  * @return Whether any row was targeted
	  */
	def deprecate()(implicit connection: Connection) = deprecationTimes = Now
	
	/**
	  * Updates the deprecatedAfter of the targeted SettingValue instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def deprecationTimes_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the fieldId of the targeted SettingValue instance(s)
	  * @param newFieldId A new fieldId to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def fieldIds_=(newFieldId: Int)(implicit connection: Connection) = putColumn(model.fieldIdColumn, 
		newFieldId)
	
	/**
	  * Updates the value of the targeted SettingValue instance(s)
	  * @param newValue A new value to assign
	  * @return Whether any SettingValue instance was affected
	  */
	def values_=(newValue: Value)(implicit connection: Connection) = putColumn(model.valueColumn, newValue)
}

