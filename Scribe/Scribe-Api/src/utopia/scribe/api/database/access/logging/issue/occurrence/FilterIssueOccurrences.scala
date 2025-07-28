package utopia.scribe.api.database.access.logging.issue.occurrence

import utopia.flow.collection.immutable.IntSet
import utopia.flow.collection.immutable.range.HasEnds
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.Last
import utopia.scribe.api.database.storable.logging.IssueOccurrenceDbModel
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * Common trait for access points which may be filtered based on issue occurrence properties
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
trait FilterIssueOccurrences[+Repr] extends FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * Model that defines issue occurrence database properties
	  */
	def model = IssueOccurrenceDbModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param caseId case id to target
	  * @return Copy of this access point that only includes issue occurrences with the specified case id
	  */
	def ofVariant(caseId: Int) = filter(model.caseId.column <=> caseId)
	/**
	  * @param caseIds Targeted case ids
	  * @return Copy of this access point that only includes issue occurrences where case id is within the 
	  * specified value set
	  */
	def ofVariants(caseIds: IterableOnce[Int]) = filter(model.caseId.column.in(IntSet.from(caseIds)))
	
	/**
	  * @param threshold          A time threshold
	  * @param includePartialRanges Whether those occurrences should be included
	  *                             where some but not all of them occurred after the specified time threshold
	  *                             (default = false)
	  * @return Access to issues that have occurred since the specified time threshold
	  */
	def since(threshold: Instant, includePartialRanges: Boolean = false) =
		filter((if (includePartialRanges) model.latest else model.earliest) > threshold)
	/**
	  * @param threshold            A time threshold
	  * @param includePartialRanges Whether those occurrences should be included
	  *                             where some but not all of them occurred before the specified time threshold
	  *                             (default = false)
	  * @return Access to instance occurrences before the specified time threshold
	  */
	def before(threshold: Instant, includePartialRanges: Boolean = false) = {
		val condition = if (includePartialRanges) model.earliest < threshold else model.latest < threshold
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
		filter(model.time(targetedEndPoint).isBetween(timeSpan.start, timeSpan.end))
}

