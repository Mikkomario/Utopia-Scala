package utopia.flow.time

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.{HasInclusiveEnds, IterableHasEnds}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration}
import utopia.flow.generic.model.mutable.DataType.LocalDateType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.DateRange.dateFormat
import utopia.flow.time.TimeExtensions._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateRange extends FromModelFactoryWithSchema[DateRange]
{
	private lazy val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
	override lazy val schema = ModelDeclaration("start" -> LocalDateType, "end" -> LocalDateType)
	
	override protected def fromValidatedModel(model: Model) =
		apply(model("start").getLocalDate, model("end").getLocalDate)
	
	/**
	  * Creates an inclusive date range
	  * @param first The first day to include
	  * @param last The last day to include
	  * @return A new date range that includes both dates
	  */
	def inclusive(first: LocalDate, last: LocalDate): DateRange = {
		if (last >= first)
			apply(first, last + 1)
		else
			apply(first, last - 1)
	}
	/**
	 * @param dates Dates to convert into a date range
	 * @return A date range containing all of the specified dates
	 */
	def apply(dates: HasInclusiveEnds[LocalDate]): DateRange = inclusive(dates.start, dates.end)
	
	/**
	  * Creates a new date range that doesn't include the end date
	  * @param start The start day (inclusive)
	  * @param end The end day (exclusive)
	  * @return A new date range
	  */
	def exclusive(start: LocalDate, end: LocalDate) = apply(start, end)
	/**
	  * Creates a new date range that doesn't include the end date
	  * @param range The date range, where the second value is excluded
	  * @return A new date range
	  */
	def exclusive(range: Pair[LocalDate]): DateRange = exclusive(range.first, range.second)
	
	/**
	  * Creates a new date range that doesn't include either the start or the end date
	  * @param start The start day (exclusive)
	  * @param end The end day (exclusive)
	  * @return A new date range that spans the dates between those two dates
	  */
	def between(start: LocalDate, end: LocalDate) = {
		if (end > start)
			apply(start + 1, end)
		else if (end < start)
			apply(start - 1, end)
		else
			apply(start, end)
	}
	
	/**
	 * @param date The date to wrap
	 * @return A date range that only contains that date
	 */
	def single(date: LocalDate) = apply(date, date + 1)
}

/**
  * Represents a specific time period. E.g. from 1st of June of 2012 to 3rd of September of 2013
  * @author Mikko Hilpinen
  * @since 11.1.2021, v1.9
  * @param start The first included date
  * @param end The first <b>excluded</b> date
  */
case class DateRange(override val start: LocalDate, override val end: LocalDate)
	extends IterableHasEnds[LocalDate] with ModelConvertible
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
	
	/**
	  * @return Whether this date range represents a single date
	  */
	def isSingleDate = nonEmpty && head == last
	
	
	// IMPLEMENTED ------------------------
	
	override def isInclusive = false
	
	override def ordering: Ordering[LocalDate] = implicitly
	
	override def toString = {
		if (isEmpty)
			"-"
		else if (head == last)
			head.format(dateFormat)
		else if (head.month == last.month)
			s"${head.dayOfMonth}-${last.dayOfMonth}.${head.monthOfYear}.${head.year}"
		else if (head.year == last.year)
			s"${head.dayOfMonth}.${head.monthOfYear}-${last.dayOfMonth}.${last.monthOfYear}.${head.year}"
		else
			s"${head.format(dateFormat)}-${last.format(dateFormat)}"
	}
	
	override def isEmpty = start == end
	
	override def head = start
	override def last = end.previous
	override def headOption = if (isEmpty) None else Some(head)
	override def lastOption = if (isEmpty) None else Some(last)
	
	override def toModel = Model(Pair("start" -> start, "end" -> end))
	
	override protected def traverse(from: LocalDate, direction: Sign) = direction match {
		case Positive => from.tomorrow
		case Negative => from.yesterday
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * @param other Another date range
	  * @return Whether this date range contains the other range completely
	  */
	def contains(other: DateRange): Boolean = other.nonEmpty && contains(other.head) && contains(other.last)
	
	/**
	  * Advances this range like a seq. Please notice that this method doesn't check whether the specified advance
	  * exceeds the length of this range. It will continue to return values as if the end of this range wasn't yet
	  * reached.
	  * @param daysAdvance Number of days to advance from the start of this range
	  * @return A date along this range
	  */
	def apply(daysAdvance: Int) = {
		// Checks the direction of advance
		if (end >= start)
			start + daysAdvance
		else
			start - daysAdvance
	}
	
	/**
	  * @param other Another date range
	  * @return Whether these two ranges overlap at some point
	  */
	def overlapsWith(other: DateRange) = other.headOption.exists(contains) ||
		other.lastOption.exists(contains) || headOption.exists(other.contains)
	/**
	  * @param other Another date range
	  * @return The overlapping portion between these two ranges. None if there is no overlap. The resulting range is
	  *         always chronological (start < end)
	  */
	def overlapWith(other: DateRange) = {
		val r1 = chronological
		val r2 = other.chronological
		
		val start = r1.start max r2.start
		val end = r1.end min r2.end
		
		if (end > start)
			Some(DateRange(start, end))
		else
			None
	}
	
	/**
	  * An operator matching the function 'overlapWith(DateRange)'
	  * @param other Another date range
	  * @return The overlapping portion between these two ranges. None if there is no overlap. The resulting range is
	  *         always chronological (start < end)
	  */
	def &(other: DateRange) = overlapWith(other)
}
