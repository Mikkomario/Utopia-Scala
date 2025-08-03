package utopia.vault.model.template

import utopia.vault.sql.Condition

/**
  * Common trait for interfaces which separate active and deprecated data based on some search condition
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
trait Deprecates
{
	// ABSTRACT -------------------------
	
	/**
	  * @return A condition that filters out all deprecated items
	  */
	def activeCondition: Condition
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return A condition that filters out all active (i.e. non-deprecated) items
	  */
	def deprecatedCondition: Condition = !activeCondition
}
