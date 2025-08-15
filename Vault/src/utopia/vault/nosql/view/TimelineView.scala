package utopia.vault.nosql.view

import utopia.flow.collection.immutable.range.HasEnds
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.TimeExtensions._
import utopia.vault.model.immutable.Column

import java.time.Instant

/**
 * Common trait for views which may be filtered based on a timestamp column
 *
 * @author Mikko Hilpinen
 * @since 15.08.2025, v2.0
 */
trait TimelineView[+Repr] extends FilterableView[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return A timestamp DB property declaration.
	 */
	protected def timestampColumn: Column
	
	
	// OTHER    -----------------------
	
	/**
	 * @param time A time threshold (exclusive)
	 * @return Access to items where the timestamp is smaller than the specified value
	 */
	def before(time: Instant) = filter(timestampColumn < time)
	/**
	 * @param to A time threshold (inclusive)
	 * @return Access to items where the timestamp is smaller or equal to the specified value
	 */
	def to(to: Instant) = filter(timestampColumn <= to)
	/**
	 * @param time A time threshold (exclusive)
	 * @return Access to items where the timestamp is greater than the specified value
	 */
	def after(time: Instant) = filter(timestampColumn > time)
	/**
	 * @param from A time threshold (inclusive)
	 * @return Access to items where the timestamp is greater or equal to the specified value
	 */
	def since(from: Instant) = filter(timestampColumn >= from)
	/**
	 * @param from Smallest included timestamp value
	 * @param to Largest included timestamp value
	 * @return Access to items where the timestamp is between the specified values (inclusive)
	 */
	def during(from: Instant, to: Instant) = filter(timestampColumn.isBetween(from, to))
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
			val t = timestampColumn
			if (timespan.start < timespan.end)
				filter(t >= timespan.start && t < timespan.end)
			else
				filter(t > timespan.end && t <= timespan.start)
		}
	}
}
