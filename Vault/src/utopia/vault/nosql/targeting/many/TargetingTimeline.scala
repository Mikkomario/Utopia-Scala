package utopia.vault.nosql.targeting.many

import utopia.flow.collection.immutable.range.HasEnds
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.time.TimeExtensions._
import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vault.sql.OrderBy

import java.time.Instant

/**
 * A common trait for targeting implementations that use a (linear) timestamp column
 *
 * @author Mikko Hilpinen
 * @since 27.06.2025, v1.21.1
 */
trait TargetingTimeline[+A, +Repr, +One] extends TargetingManyLike[A, Repr, One]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return A timestamp DB property declaration.
	 */
	protected def timestamp: DbPropertyDeclaration
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return Access to the earliest entry, based on the timestamp column
	 */
	def earliest = timeline(First)
	/**
	 * @return Access to the latest entry, based on the timestamp column
	 */
	def latest = timeline(Last)
	
	
	// OTHER    ----------------------
	
	/**
	 * @param end Targeted end of the timeline
	 * @return Access to the specified end of items, when ordering by the timestamp column (chronologically)
	 */
	def timeline(end: End) = apply(end, Some(OrderBy.ascending(timestamp)))
	
	/**
	 * @param time A time threshold (exclusive)
	 * @return Access to items where the timestamp is smaller than the specified value
	 */
	def before(time: Instant) = filter(timestamp < time)
	/**
	 * @param to A time threshold (inclusive)
	 * @return Access to items where the timestamp is smaller or equal to the specified value
	 */
	def to(to: Instant) = filter(timestamp <= to)
	/**
	 * @param time A time threshold (exclusive)
	 * @return Access to items where the timestamp is greater than the specified value
	 */
	def after(time: Instant) = filter(timestamp > time)
	/**
	 * @param from A time threshold (inclusive)
	 * @return Access to items where the timestamp is greater or equal to the specified value
	 */
	def since(from: Instant) = filter(timestamp >= from)
	/**
	 * @param from Smallest included timestamp value
	 * @param to Largest included timestamp value
	 * @return Access to items where the timestamp is between the specified values (inclusive)
	 */
	def during(from: Instant, to: Instant) = filter(timestamp.isBetween(from, to))
	/**
	 * @param timespan Targeted timespan
	 * @return Access to items where the timestamp is on the specified timespan
	 */
	def during(timespan: HasEnds[Instant]): Repr = {
		// Case: Inclusive range => Uses BETWEEN
		if (timespan.isInclusive)
			during(timespan.start, timespan.end)
		// Case: Exclusive range => Uses > and <
		else {
			val t = timestamp.column
			if (timespan.start < timespan.end)
				filter(t >= timespan.start && t < timespan.end)
			else
				filter(t > timespan.end && t <= timespan.start)
		}
	}
}
