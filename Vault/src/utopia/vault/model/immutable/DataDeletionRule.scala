package utopia.vault.model.immutable

import utopia.flow.operator.MaybeEmpty
import utopia.flow.time.Duration
import utopia.vault.model.template.HasTable
import utopia.vault.sql.Condition

object DataDeletionRule
{
	/**
	  * Creates a new empty deletion rule that can then be appended
	  * @param table Target table
	  * @param timePropertyName Name of the property that contains row creation time
	  * @return An empty deletion rule which has no effect by itself but can be expanded / appended
	  */
	@deprecated("Please use .empty(TableColumn) instead", "v2.0")
	def empty(table: Table, timePropertyName: String) = DataDeletionRule(table(timePropertyName))
	/**
	  * Creates a new empty deletion rule that can then be appended
	  * @param timeColumn Column that contains a reference time for the deletion rules
	  * @return An empty deletion rule which has no effect by itself but can be expanded / appended
	  */
	def empty(timeColumn: TableColumn) = apply(timeColumn)
	
	/**
	  * Creates a new conditional deletion rule
	  * @param table                   Target table
	  * @param timePropertyName        Name of the property that contains row creation time
	  * @param condition               Condition that must be true for rows to be deleted
	  * @param liveDurationOnCondition How long the rows should be allowed to exist before the specified
	  *                                deletion condition is even applied
	  * @return A new rule that deletes rows based on the specified condition and minimum live duration
	  */
	@deprecated("Please use .conditional(TableColumn, Condition, Duration) instead", "v2.0")
	def conditional(table: Table, timePropertyName: String, condition: Condition,
	                liveDurationOnCondition: Duration): DataDeletionRule =
		conditional(table(timePropertyName), condition, liveDurationOnCondition)
	/**
	  * Creates a new conditional deletion rule
	  * @param timeColumn Column that contains a reference time for the deletion rules
	  * @param condition Condition that must be true for rows to be deleted
	  * @param liveDurationOnCondition How long the rows should be allowed to exist before the specified
	  *                                deletion condition is even applied
	  * @return A new rule that deletes rows based on the specified condition and minimum live duration
	  */
	def conditional(timeColumn: TableColumn, condition: Condition, liveDurationOnCondition: Duration): DataDeletionRule =
	{
		val conditions = {
			if (condition.isAlwaysFalse)
				Map[Condition, Duration]()
			else
				Map(condition -> liveDurationOnCondition)
		}
		apply(timeColumn, conditionalLiveDurations = conditions)
	}
	
	/**
	 * @param table Target table
	 * @param expirationPropertyName Name of the expiration property
	 * @return A deletion rule that deletes items immediately when the value in the targeted time column is reached
	 */
	@deprecated("Please use .at(TableColumn) instead", "v2.0")
	def onArrivalOf(table: Table, expirationPropertyName: String) =
		at(table(expirationPropertyName))
	/**
	  * @param timeColumn A column that, when its timestamp value is reached, will cause the row to be deleted
	  * @return A deletion rule that deletes a row when a timestamp column's value is reached
	  */
	def at(timeColumn: TableColumn) = apply(timeColumn, standardLiveDuration = Duration.zero)
}

/**
  * Used for specifying a condition for automated data deletion
  * @author Mikko Hilpinen
  * @since 30.7.2020, v1.6
  * @param timeColumn Column that determines the reference time for the defined live-durations
  * @param standardLiveDuration How long each row should be stored
  *                             (infinite if data should be stored forever by default (default))
  * @param conditionalLiveDurations Conditional exceptions to apply to the standard live duration (default = empty)
  * @see utopia.vault.database.ClearOldData
  */
case class DataDeletionRule(timeColumn: TableColumn,
                            standardLiveDuration: Duration = Duration.infinite,
                            conditionalLiveDurations: Map[Condition, Duration] = Map())
	extends MaybeEmpty[DataDeletionRule] with HasTable
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * @return Whether this deletion rule has no effect on the targeted table
	  *         (no row live durations have been specified)
	  */
	lazy val isEmpty = !standardLiveDuration.isFinite && conditionalLiveDurations.isEmpty
	
	
	// COMPUTED -----------------------------
	
	@deprecated("Deprecated for removal. Please use .table instead", "v2.0")
	def targetTable = table
	@deprecated("Deprecated for removal. Please use .timeColumn instead", "v2.0")
	def timePropertyName = timeColumn.name
	
	
	// IMPLEMENTED	-------------------------
	
	override def self = this
	
	override def table: Table = timeColumn.table
	
	
	// OTHER	-------------------------
	
	/**
	  * Adds a new conditional deletion to this rule
	  * @param conditionalDeletion A condition live duration pair
	  * @return A copy of this deletion rule set that deletes target rows id they fulfill the specified condition and
	  *         have lived at least the specified time period
	  */
	def +(conditionalDeletion: (Condition, Duration)) = {
		// Won't include conditions that can never be met
		if (conditionalDeletion._1.isAlwaysFalse)
			this
		else
			copy(conditionalLiveDurations = conditionalLiveDurations + conditionalDeletion)
	}
	
	/**
	  * @param condition Deletion condition
	  * @param liveDurationOnCondition Minimum live duration for rows where the condition applies
	  * @return A copy of this deletion rule set that deletes target rows id they fulfill the specified condition and
	  *         have lived at least the specified time period
	  */
	def withConditionalDeletion(condition: Condition, liveDurationOnCondition: Duration) =
		this + (condition -> liveDurationOnCondition)
	
	/**
	  * @param duration Unconditional live duration
	  * @return A copy of this deletion rule where rows are normally deleted after they've existed for the
	  *         specified duration
	  */
	def withStandardLiveDuration(duration: Duration) = copy(standardLiveDuration = duration)
}
