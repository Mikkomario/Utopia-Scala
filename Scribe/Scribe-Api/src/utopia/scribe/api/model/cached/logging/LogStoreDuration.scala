package utopia.scribe.api.model.cached.logging

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.operator.MaybeEmpty
import utopia.scribe.api.model.enumeration.CleanupOperation
import utopia.scribe.api.model.enumeration.CleanupOperation.{Delete, Merge}

import scala.concurrent.duration.Duration

object LogStoreDuration
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * Set of durations that doesn't apply merging or deletion at all
	  */
	val unspecified = LogStoreDuration()
	
	
	// OTHER    -------------------------
	
	/**
	  * @param thresholds Thresholds (first value) after which issue occurrences are merged together in groups of a
	  *                   specified time period (second value)
	  *
	  *                   E.g. If the first value is 7 days and the second value is 1 day,
	  *                   Occurrences that are all 7 days old or older will be grouped into sequences that last
	  *                   up to 1 day. I.e. there will be only one occurrence entry per each day for older entries.
	  *
	  *                   The values should be specified in descending order (based on the first value).
	  *                   The longest applicable threshold will be used when merging issues.
	  *
	  * @return A set of durations that to be applied for issue merging
	  */
	def forMerge(thresholds: Seq[Pair[Duration]]) = apply(Map(Merge -> thresholds))
	
	/**
	  * @param thresholds Inactive duration thresholds (first value)
	  *                   and matching occurrence lifetime thresholds (second value),
	  *                   that determine when issue occurrences will be deleted from the database entirely.
	  *
	  *                   E.g. If the first value is 7 days and the second value is 4 weeks,
	  *                   occurrences will be deleted once they're all 4 weeks old or older,
	  *                   provided that there haven't been any occurrences of that issue (variant) within
	  *                   the last 7 days.
	  *
	  *                   The values should be specified in descending order (based on the first value).
	  *                   The longest applicable inactivity threshold will be used when deleting issues.
	  *
	  * @return A set of durations that are to be applied for issue deletion
	  */
	def forDeletion(thresholds: Seq[Pair[Duration]]) = apply(Map(Delete -> thresholds))
}

/**
  * Used for specifying how long a group of issues is to be stored in the database,
  * and in which form (i.e. when and how merging occurs)
  * @author Mikko Hilpinen
  * @since 9.7.2023, v1.0
  * @constructor Constructs a new set of store duration rules
  * @param thresholds Set of time thresholds to apply to different cleanup operations.
  *                   Each threshold is specified as a set of two duration values. The role of these values
  *                   differs by the targeted cleanup operation. These roles are explained below.
  *                   Multiple threshold values may be specified for each operation type.
  *                   When multiple values are specified, they should be given in descending order
  *                   (based on their first value), as the first applicable threshold will always be used.
  *                   The lists of thresholds are specified as a single map where keys are the targeted cleanup
  *                   operations.
  *
  *                   **Thresholds for merging:**
  *                   Thresholds (first value) after which issue occurrences are merged together in groups of a
  *                   specified time period (second value)
  *
  *                   E.g. If the first value is 7 days and the second value is 1 day,
  *                   Occurrences that are all 7 days old or older will be grouped into sequences that last
  *                   up to 1 day. I.e. there will be only one occurrence entry per each day for older entries.
  *
  *                   **Thresholds for deletion:**
  *                   Inactive duration thresholds (first value)
  *                   and matching occurrence lifetime thresholds (second value),
  *                   that determine when issue occurrences will be deleted from the database entirely.
  *
  *                   E.g. If the first value is 7 days and the second value is 4 weeks,
  *                   occurrences will be deleted once they're all 4 weeks old or older,
  *                   provided that there haven't been any occurrences of that issue (variant) within
  *                   the last 7 days.
  */
case class LogStoreDuration(thresholds: Map[CleanupOperation, Seq[Pair[Duration]]] = Map())
	extends MaybeEmpty[LogStoreDuration]
{
	// COMPUTED ----------------------
	
	/**
	  * @return Time thresholds for merging, where the first value is occurrence oldness and second value is
	  *         applied merge group length
	  */
	def mergingThresholds = apply(Merge)
	/**
	  * @return Time thresholds for deletion, where the first value is required inactivity period and the second
	  *         value is applied maximum occurrence lifetime
	  */
	def deletionThresholds = apply(Delete)
	
	/**
	  * @return Different merge group sizes for different occurrence time ranges
	  */
	def mergeDurationRangesIterator = rangesIterator(Merge)
	/**
	  * @return Different occurrence lifetime durations for different inactivity periods
	  */
	def deletionRangesIterator = rangesIterator(Delete)
	
	
	// IMPLEMENTED  ------------------
	
	override def self: LogStoreDuration = this
	override def isEmpty: Boolean = thresholds.forall { _.isEmpty }
	
	
	// OTHER    ----------------------
	
	/**
	  * @param operation Targeted cleanup operation type
	  * @return Thresholds that apply to that operation type
	  */
	def apply(operation: CleanupOperation) = thresholds.getOrElse(operation, Empty)
	/**
	  * @param operation Targeted cleanup operation type
	  * @param duration Applicable store or inactivity duration (role depends on the targeted operation)
	  * @return Applicable merging or deletion duration, depending on the targeted type
	  */
	def apply(operation: CleanupOperation, duration: Duration) =
		thresholds.get(operation).flatMap { _.find { _.first <= duration }.map { _.second } }
	
	/**
	  * @param operation Targeted cleanup operation
	  * @return A sequence of time ranges for which a different duration has been specified,
	  *         along with the applied duration.
	  *         The nature of these durations is dependent on the targeted operation.
	  */
	def rangesIterator(operation: CleanupOperation): Iterator[(Span[Duration], Duration)] = {
		val thresholds = apply(operation)
		if (thresholds.isEmpty)
			Iterator.empty
		else {
			val largest = thresholds.head
			// Converts the thresholds into ranges
			val rangesIter = (Span(Duration.Inf, largest.first), largest.second) +:
				thresholds.iterator.paired.map { case Pair(larger, smaller) =>
					Span(larger.first, smaller.first) -> larger.second
				}
			// Merges consecutive ranges together where applicable
			rangesIter.groupBy { _._2 }.map { case (duration, ranges) =>
				val mergedRange = ranges.oneOrMany match {
					case Left((onlyRange, _)) => onlyRange
					case Right(ranges) => Span(ranges.head._1.start, ranges.last._1.end)
				}
				mergedRange -> duration
			}
		}
	}
	
	/**
	  * @param operation A cleanup operation for which these thresholds are added
	  * @param thresholds Thresholds applied to the specified operation.
	  *                   The role of these thresholds depends on the targeted operation.
	  * @return Copy of this set with the specified thresholds ADDED
	  */
	def withThresholds(operation: CleanupOperation, thresholds: IterableOnce[Pair[Duration]]) =
		copy(thresholds = this.thresholds + (operation -> (apply(operation) ++ thresholds).reverseSortBy { _.first }))
	/**
	  * @param threshold Issue occurrence lifetime threshold + merge group length
	  * @return Copy of this set with the specified merge threshold added
	  */
	def withThreshold(operation: CleanupOperation, threshold: Pair[Duration]) =
		copy(thresholds = this.thresholds + (operation -> (apply(operation) :+ threshold).reverseSortBy { _.first }))
	
	/**
	  * @param thresholds Thresholds after which merging should occur + merged group lengths
	  * @return Copy of this set with the specified thresholds ADDED
	  */
	def withMergeAfterThresholds(thresholds: IterableOnce[Pair[Duration]]) =
		withThresholds(Merge, thresholds)
	/**
	  * @param thresholds Issue occurrence lifetime threshold + merge group length
	  * @return Copy of this set with the specified merge threshold added
	  */
	def withMergeAfter(thresholds: Pair[Duration]) = withThreshold(Merge, thresholds)
	/**
	  * @param threshold Issue occurrence lifetime threshold after which this rule should be applied
	  * @param groupSize Length of the resulting merge groups
	  * @return Copy of this set with the specified merge threshold added
	  */
	def withMergeAfter(threshold: Duration, groupSize: Duration): LogStoreDuration =
		withMergeAfter(Pair(threshold, groupSize))
	
	/**
	  * @param thresholds Inactivity time thresholds after which a rule should be applied +
	  *                   applied issue occurrence lifetime durations
	  * @return Copy of this set with the specified thresholds ADDED
	  */
	def withDeleteAfterThresholds(thresholds: IterableOnce[Pair[Duration]]) =
		withThresholds(Delete, thresholds)
	/**
	  * @param thresholds Issue inactivity threshold + applied issue lifetime duration
	  * @return Copy of this set with the specified merge threshold added
	  */
	def withDeleteAfter(thresholds: Pair[Duration]) = withThreshold(Delete, thresholds)
	/**
	  * @param minimumInactivity Issue inactivity threshold
	  * @param lifeTime Applied issue lifetime duration
	  * @return Copy of this set with the specified merge threshold added
	  */
	def withDeleteAfter(minimumInactivity: Duration, lifeTime: Duration): LogStoreDuration =
		withDeleteAfter(Pair(minimumInactivity, lifeTime))
	/**
	  * @param threshold Duration after which all issue occurrences should be deleted,
	  *                  regardless of whether they're active or not.
	  * @return Copy of this set with the specified (maximum) issue lifetime duration applied.
	  */
	def deletingAnythingAfter(threshold: Duration) = withDeleteAfter(Duration.Zero, threshold)
	
	/**
	  * @param other Another set of durations
	  * @return Copy of these sets where both are applied
	  */
	def ++(other: LogStoreDuration) =
		copy(thresholds = thresholds.mergeWith(other.thresholds) { (a, b) => (a ++ b).reverseSortBy { _.first } })
}