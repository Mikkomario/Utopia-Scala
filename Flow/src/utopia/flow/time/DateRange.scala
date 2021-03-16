package utopia.flow.time

import TimeExtensions._

import java.time.LocalDate

object DateRange
{
	/**
	  * Creates an inclusive date range
	  * @param first The first day to include
	  * @param last The last day to include
	  * @return A new date range that includes both dates
	  */
	def inclusive(first: LocalDate, last: LocalDate) =
	{
		if (last >= first)
			apply(first, last + 1)
		else
			apply(first, last - 1)
	}
	
	/**
	  * Creates a new date range that doesn't include the end date
	  * @param start The start day (inclusive)
	  * @param end The end day (exclusive)
	  * @return A new date range
	  */
	def exclusive(start: LocalDate, end: LocalDate) = apply(start, end)
	
	/**
	  * Creates a new date range that doesn't include either the start or the end date
	  * @param start The start day (exclusive)
	  * @param end The end day (exclusive)
	  * @return A new date range that spans the dates between those two dates
	  */
	def between(start: LocalDate, end: LocalDate) =
	{
		if (end > start)
			apply(start + 1, end)
		else if (end < start)
			apply(start - 1, end)
		else
			apply(start, end)
	}
}

/**
  * Represents a specific time period. E.g. from 1st of June of 2012 to 3rd of September of 2013
  * @author Mikko Hilpinen
  * @since 11.1.2021, v1.9
  * @param start The first included date
  * @param end The first <b>excluded</b> date
  */
case class DateRange(start: LocalDate, end: LocalDate) extends Iterable[LocalDate]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * Whether this range iterates dates chronologically forward (true) or backward (false)
	  */
	lazy val isChronological = end >= start
	/**
	  * The length of this range as a time period
	  */
	lazy val period = end - start
	
	
	// COMPUTED ---------------------------
	
	/**
	  * @return Whether this range iterates dates in backwards chronological order
	  */
	def nonChronological = !isChronological
	
	/**
	  * @return This range in reverse order
	  */
	def reverse = if (isChronological) DateRange(end - 1, start - 1) else DateRange(end + 1, start + 1)
	
	/**
	  * @return This date range in chronological (increasing) order
	  */
	def chronological = if (isChronological) this else reverse
	/**
	  * @return This date range in inverse chronological (decreasing) order
	  */
	def inverseChronological = if (isChronological) reverse else this
	
	
	// IMPLEMENTED ------------------------
	
	override def isEmpty = start == end
	
	override def head = start
	
	override def last = end.previous
	
	override def headOption = if (isEmpty) None else Some(head)
	
	override def lastOption = if (isEmpty) None else Some(last)
	
	override def iterator =
	{
		// Checks the direction of advance
		if (end >= start)
			Iterator.iterate(start) { _.next }.takeWhile { _ < end }
		else
			Iterator.iterate(last) { _.previous }.takeWhile { _ >= start }
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * @param date Target date
	  * @return Whether this date range contains the specified date
	  */
	def contains(date: LocalDate) =
		if (isChronological) date >= start && date < end else date > end && date <= start
	
	/**
	  * Advances this range like a seq. Please notice that this method doesn't check whether the specified advance
	  * exceeds the length of this range. It will continue to return values as if the end of this range wasn't yet
	  * reached.
	  * @param daysAdvance Number of days to advance from the start of this range
	  * @return A date along this range
	  */
	def apply(daysAdvance: Int) =
	{
		// Checks the direction of advance
		if (end >= start)
			start + daysAdvance
		else
			start - daysAdvance
	}
}
