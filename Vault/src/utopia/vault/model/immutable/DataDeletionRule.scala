package utopia.vault.model.immutable

import java.time.Period

import utopia.vault.sql.Condition

object DataDeletionRule
{
	/**
	  * Creates a new empty deletion rule that can then be appended
	  * @param table Target table
	  * @param timePropertyName Name of the property that contains row creation time
	  * @return An empty deletion rule which has no effect by itself but can be expanded / appended
	  */
	def empty(table: Table, timePropertyName: String) = DataDeletionRule(table, timePropertyName)
	
	/**
	  * Creates a new deletion rule with unconditional live duration
	  * @param table Target table
	  * @param timePropertyName Name of the property that contains row creation time
	  * @param standardLiveDuration How long each row should be stored
	  * @return A new rule that deletes all table rows once they've existed for specified duration
	  */
	def apply(table: Table, timePropertyName: String, standardLiveDuration: Period): DataDeletionRule =
		DataDeletionRule(table, timePropertyName, Some(standardLiveDuration))
	
	/**
	  * Creates a new conditional deletion rule
	  * @param table Target table
	  * @param timePropertyName Name of the property that contains row creation time
	  * @param condition Condition that must be true for rows to be deleted
	  * @param liveDurationOnCondition How long the rows should be allowed to exist before the specified
	  *                                deletion condition is even applied
	  * @return A new rule that deletes rows based on the specified condition and minimum live duration
	  */
	def conditional(table: Table, timePropertyName: String, condition: Condition, liveDurationOnCondition: Period) =
		DataDeletionRule(table, timePropertyName, conditionalLiveDurations = Map(condition -> liveDurationOnCondition))
}

/**
  * Used for specifying a condition for automated data deletion
  * @author Mikko Hilpinen
  * @since 30.7.2020, v1.6
  * @see utopia.vault.database.ClearOldData
  */
case class DataDeletionRule(targetTable: Table, timePropertyName: String, standardLiveDuration: Option[Period] = None,
							conditionalLiveDurations: Map[Condition, Period] = Map())
{
	// COMPUTED	-------------------------
	
	/**
	  * @return Whether this deletion rule has no effect on the targeted table
	  *         (no row live durations have been specified)
	  */
	def isEmpty = standardLiveDuration.isEmpty && conditionalLiveDurations.isEmpty
	
	/**
	  * @return Whether this deletion rule has an effect on the targeted table under certain conditions.
	  */
	def nonEmpty = !isEmpty
	
	
	// OTHER	-------------------------
	
	/**
	  * Adds a new conditional deletion to this rule
	  * @param conditionalDeletion A condition live duration pair
	  * @return A copy of this deletion rule set that deletes target rows id they fulfill the specified condition and
	  *         have lived at least the specified time period
	  */
	def +(conditionalDeletion: (Condition, Period)) =
		copy(conditionalLiveDurations = conditionalLiveDurations + conditionalDeletion)
	
	/**
	  * @param condition Deletion condition
	  * @param liveDurationOnCondition Minimum live duration for rows where the condition applies
	  * @return A copy of this deletion rule set that deletes target rows id they fulfill the specified condition and
	  *         have lived at least the specified time period
	  */
	def withConditionalDeletion(condition: Condition, liveDurationOnCondition: Period) =
		this + (condition -> liveDurationOnCondition)
	
	/**
	  * @param duration Unconditional live duration
	  * @return A copy of this deletion rule where rows are normally deleted after they've existed for the
	  *         specified duration
	  */
	def withStandardLiveDuration(duration: Period) = copy(standardLiveDuration = Some(duration))
}
