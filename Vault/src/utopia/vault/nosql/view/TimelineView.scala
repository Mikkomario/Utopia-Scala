package utopia.vault.nosql.view

import utopia.flow.collection.immutable.range.{HasEnds, HasOrderedEnds, MayHaveOrderedEnds}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.TimeExtensions._
import utopia.vault.model.immutable.Column
import utopia.vault.sql.Condition

import java.time.Instant

object TimelineView
{
	// EXTENSIONS   ---------------------
	
	implicit class DeprecatingTimelineView[+Repr](val v: TimelineView[Repr] with TimeDeprecatableView[Repr])
		extends AnyVal
	{
		/**
		 * @param instant A timestamp
		 * @return Access to items that were in a non-deprecated state at the specified instant
		 */
		def activeDuring(instant: Instant): Repr = {
			val deprCol = v.model.deprecationColumn
			val deprecationCondition = {
				if (deprCol.allowsNull)
					deprCol.isNull || deprCol < instant
				else
					deprCol < instant
			}
			v.filter(v.timestampColumn <= instant && deprecationCondition)
		}
		
		/**
		 * @param threshold A time threshold (inclusive)
		 * @return Access to items that were either created or deprecated since the specified time threshold
		 */
		def modifiedSince(threshold: Instant): Repr =
			v.filter(v.timestampColumn >= threshold || v.model.deprecatedSinceCondition(threshold))
	}
}

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
	def during(timespan: HasOrderedEnds[Instant]): Repr = {
		// Case: Inclusive range => Uses BETWEEN
		if (timespan.isInclusive)
			during(timespan.start, timespan.end)
		// Case: Exclusive range => Uses > and <
		else {
			if (timespan.isEmpty)
				filter(Condition.alwaysFalse)
			else {
				val t = timestampColumn
				if (timespan.isAscending)
					filter(t >= timespan.start && t < timespan.end)
				else
					filter(t > timespan.end && t <= timespan.start)
			}
		}
	}
	/**
	 * @param timespan Targeted timespan
	 * @return Access to items where the timestamp is on the specified timespan
	 */
	def during(timespan: HasEnds[Instant]): Repr = timespan match {
		case s: HasOrderedEnds[Instant] => during(s)
		case other => during(HasOrderedEnds.from(other))
	}
	/**
	 * @param timespan Targeted timespan, which may be open
	 * @return Access to items where the timestamp is on the specified timespan
	 */
	def during(timespan: MayHaveOrderedEnds[Instant]): Repr = {
		// Case: Empty range => Can't access any items
		if (timespan.isEmpty)
			filter(Condition.alwaysFalse)
		else
			timespan.startOption match {
				case Some(start) =>
					timespan.endOption match {
						// Case: Closed range
						case Some(end) =>
							// Case: Inclusive range => Uses IS BETWEEN
							if (timespan.isInclusive)
								during(start, end)
							// Case: Exclusive range => Applies < or > for the end value
							else {
								val t = timestampColumn
								if (timespan.isAscending)
									filter(t >= start && t < end)
								else
									filter(t <= start && t > end)
							}
						// Case: Open end => Applies >= or <=
						case None =>
							if (timespan.isAscending)
								filter(timestampColumn >= start)
							else
								filter(timestampColumn <= start)
					}
				case None =>
					timespan.endOption match {
						// Case: Open start => Applies inclusive or exclusive column comparison
						case Some(end) =>
							if (timespan.isInclusive) {
								if (timespan.isAscending)
									filter(timestampColumn <= end)
								else
									filter(timestampColumn >= end)
							}
							else if (timespan.isAscending)
								filter(timestampColumn < end)
							else
								filter(timestampColumn > end)
						
						// Case: Infinite range => Won't filter
						case None => self
					}
			}
	}
}
