package utopia.vault.nosql.targeting.many

import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.vault.nosql.view.TimelineView
import utopia.vault.sql.OrderBy

/**
 * A common trait for targeting implementations that use a (linear) timestamp column
 *
 * @author Mikko Hilpinen
 * @since 27.06.2025, v1.21.1
 */
trait TargetingTimeline[+A, +Repr, +One] extends TargetingManyLike[A, Repr, One] with TimelineView[Repr]
{
	// COMPUTED ----------------------
	
	/**
	 * @return Access to the earliest entry, based on the timestamp column
	 */
	def earliest = timeline(First)
	/**
	 * @return Access to the latest entry, based on the timestamp column
	 */
	def latest = timeline(Last)
	
	/**
	 * @return A copy of this access, which orders the accessed items chronologically
	 */
	def chronological = withOrdering(OrderBy.ascending(timestampColumn))
	/**
	 * @return A copy of this access, which orders the accessed items from newest to oldest
	 */
	def latestToEarliest = withOrdering(OrderBy.descending(timestampColumn))
	
	
	// OTHER    ----------------------
	
	/**
	 * @param end Targeted end of the timeline
	 * @return Access to the specified end of items, when ordering by the timestamp column (chronologically)
	 */
	def timeline(end: End) = apply(end, Some(OrderBy.ascending(timestampColumn)))
}
