package utopia.vault.model.template

import utopia.vault.sql.Condition

/**
 * Common trait for elements that support join conditions
 * @author Mikko Hilpinen
 * @since 28.11.2025, v2.1
 */
trait ConditionallyJoinable[+Repr]
{
	// ABSTRACT ----------------------
	
	/**
	 * @param condition A condition to apply for joining
	 * @return A copy of this element requiring the specified join condition
	 */
	def onlyJoinIf(condition: Condition): Repr
}
