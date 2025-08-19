package utopia.vault.model.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.sql.Condition

object Deprecates
{
	// OTHER    -------------------------
	
	/**
	 * @param condition A static condition that marks an entry as active / valid
	 * @return A wrapper for that condition
	 */
	def activeIf(condition: Condition) =
		basedOnConditions(View.fixed(condition), Lazy { !condition })
	/**
	 * @param activeConditionView A variable condition that marks an entry as active / valid
	 * @return A wrapper for that condition
	 */
	def activeIf(activeConditionView: View[Condition]) =
		basedOnConditions(activeConditionView, activeConditionView.mapValue { !_ })
	
	/**
	 * @param activeCondition     A static condition that marks an entry as active / valid
	 * @param deprecatedCondition A static condition that marks an entry as inactive / historical / deprecated
	 * @return A wrapper for those conditions
	 */
	def basedOnConditions(activeCondition: Condition, deprecatedCondition: Condition): Deprecates =
		basedOnConditions(View.fixed(activeCondition), View.fixed(deprecatedCondition))
	/**
	 * @param activeConditionView A variable condition that marks an entry as active / valid
	 * @param deprecatedConditionView A variable condition that marks an entry as inactive / historical / deprecated
	 * @return A wrapper for those conditions
	 */
	def basedOnConditions(activeConditionView: View[Condition],
	                      deprecatedConditionView: View[Condition]): Deprecates =
		new _Deprecates(activeConditionView, deprecatedConditionView)
	
	/**
	 * @param first The first set of conditions
	 * @param second The second set of conditions
	 * @param more More conditions
	 * @return An interface that considers a row deprecated if any of the specified instances considers it deprecated
	 */
	def combine(first: Deprecates, second: Deprecates, more: Deprecates*): Deprecates =
		combine(Pair(first, second) ++ more)
	/**
	 * Combines 0-n deprecation conditions together
	 * @param parts Parts to combine
	 * @return An interface that considers a row deprecated if any of the specified instances considers it deprecated
	 */
	def combine(parts: Seq[Deprecates]): Deprecates = parts.emptyOneOrMany match {
		case None => basedOnConditions(Condition.alwaysTrue, Condition.alwaysFalse)
		case Some(Left(only)) => only
		case Some(Right(many)) => new CombiningDeprecates(many)
	}
	
	
	// NESTED   -------------------------
	
	private class _Deprecates(activeConditionView: View[Condition], deprecatedConditionView: View[Condition])
		extends Deprecates
	{
		override def activeCondition: Condition = activeConditionView.value
		override def deprecatedCondition: Condition = deprecatedConditionView.value
	}
	
	private class CombiningDeprecates(parts: Seq[Deprecates]) extends Deprecates
	{
		override def activeCondition: Condition = Condition.and(parts.map { _.activeCondition })
	}
}

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
