package utopia.vault.model.template

import utopia.flow.collection.immutable.Pair
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
	 * @param condition A condition that must be met for joining to occur
	 * @return A copy of this element requiring the specified join condition
	 */
	def onlyJoinIf(condition: Condition): Repr
	/**
	 * @param conditions Conditions that must (all) be met in order for joining to occur
	 * @return A copy of this element, requiring the specified join conditions
	 */
	def onlyJoinIf(conditions: Seq[Condition]): Repr
	
	
	// OTHER    --------------------
	
	/**
	 * @param firstCondition A condition that must be met in order for joining to occur
	 * @param secondCondition Another condition that must also be met
	 * @param moreConditions More conditions required for joining (optional)
	 * @return A copy of this element, requiring the specified join conditions
	 */
	def onlyJoinIf(firstCondition: Condition, secondCondition: Condition, moreConditions: Condition*): Repr =
		onlyJoinIf(Pair(firstCondition, secondCondition) ++ moreConditions)
}
