package utopia.scribe.database.access.single.settings

import utopia.scribe.database.access.many.settings.DbSettingValues
import utopia.scribe.model.stored.settings.SettingField
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual SettingFields, based on their id
  * @author Mikko Hilpinen
  * @since 12.12.2021, v0.2
  */
case class DbSingleSettingField(id: Int) 
	extends UniqueSettingFieldAccess with SingleIntIdModelAccess[SettingField]
{
	// COMPUTED -------------------------
	
	/**
	  * @return An access point to this field's values
	  */
	def valuesAccess = DbSettingValues.ofFieldWithId(id)
	/**
	  * @return An access point to this field's latest value
	  */
	def valueAccess = DbSettingValue.ofFieldWithId(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return The current value of this field
	  */
	def value(implicit connection: Connection) = valueAccess.value
	/**
	  * @param connection Implicit DB Connection
	  * @return Current values of this field
	  */
	def values(implicit connection: Connection) = valuesAccess.values
}
