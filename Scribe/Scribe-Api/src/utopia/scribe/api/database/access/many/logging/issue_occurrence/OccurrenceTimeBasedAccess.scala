package utopia.scribe.api.database.access.many.logging.issue_occurrence

import utopia.flow.collection.immutable.range.HasEnds
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.enumeration.End.Last
import utopia.flow.operator.enumeration.End
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * Common trait for access points that allow targeting based on issue occurrence time
  * @author Mikko Hilpinen
  * @since 9.7.2023, v1.0
  */
trait OccurrenceTimeBasedAccess[+Repr] extends FilterableView[Repr]
{
	// COMPUTED ------------------------
	
	private def _model = IssueOccurrenceModel
	
	
	// OTHER    -----------------------
	
	/**
	  * @param threshold          A time threshold
	  * @param includePartialRanges Whether those occurrences should be included
	  *                             where some but not all of them occurred after the specified time threshold
	  *                             (default = false)
	  * @return Access to issues that have occurred since the specified time threshold
	  */
	def since(threshold: Instant, includePartialRanges: Boolean = false) =
		filter((if (includePartialRanges) _model.latestColumn else _model.earliestColumn) > threshold)
	/**
	  * @param threshold            A time threshold
	  * @param includePartialRanges Whether those occurrences should be included
	  *                             where some but not all of them occurred before the specified time threshold
	  *                             (default = false)
	  * @return Access to instance occurrences before the specified time threshold
	  */
	def before(threshold: Instant, includePartialRanges: Boolean = false) = {
		val condition = if (includePartialRanges) _model.earliestColumn <
			threshold else _model.latestColumn < threshold
		filter(condition)
	}
	
	/**
	  * @param timeSpan         Targeted time-span
	  * @param targetedEndPoint The targeted side of occurrence periods.
	  *                         Last (default) for last occurrence times,
	  *                         First for first occurrence times.
	  * @return Access to issues that have occurred (partially) during the specified timespan
	  */
	def during(timeSpan: HasEnds[Instant], targetedEndPoint: End = Last) =
		filter(_model.timeColumn(targetedEndPoint).isBetween(timeSpan.start, timeSpan.end))
}
