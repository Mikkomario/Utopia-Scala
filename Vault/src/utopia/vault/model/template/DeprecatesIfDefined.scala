package utopia.vault.model.template

import utopia.vault.model.immutable.{Column, TableColumn}
import utopia.vault.sql.Condition

/**
  * Common trait for interfaces which separate rows into active and deprecated,
  * based on whether they specify a value in a "deprecation column"
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
trait DeprecatesIfDefined extends Deprecates
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A column that, if set / contains a value, marks the row as deprecated
	  */
	def deprecationColumn: TableColumn
	
	
	// IMPLEMENTED  --------------------
	
	override def activeCondition: Condition = deprecationColumn.isNull
	override def deprecatedCondition = deprecationColumn.isNotNull
}
