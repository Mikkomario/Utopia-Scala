package utopia.flow.time

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.YearMonthType
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Month.{December, January}
import utopia.flow.util.StringExtensions._

import java.time.{DateTimeException, LocalDate}
import scala.language.implicitConversions

object YearMonth
{
	// TYPES    ---------------------------
	
	type JYearMonth = java.time.YearMonth
	
	
	// IMPLICIT ---------------------------
	
	implicit def apply(yearMonth: JYearMonth): YearMonth = apply(Year(yearMonth.getYear), Month(yearMonth.getMonthValue))
	implicit def toJava(yearMonth: YearMonth): JYearMonth = yearMonth.toJava
}

/**
 * Represents a month at a specific year
 *
 * @author Mikko Hilpinen
 * @since 04.06.2025, v2.7
 */
case class YearMonth(year: Year, month: Month) extends MonthLike[YearMonth] with ValueConvertible
{
	// COMPUTED ----------------------------
	
	/**
	  * @return The first day of this month
	  */
	def firstDay = apply(1)
	/**
	  * @return The last day of this month
	  */
	def lastDay = month.lastDateAt(year)
	/**
	  * @return Dates in this month as a date range
	  */
	def dates = DateRange.inclusive(firstDay, lastDay)
	/**
	  * @return An iterator that iterates through the dates in this month
	  */
	def datesIterator = month.datesIteratorAt(year)
	
	/**
	  * @return A Java version of this instance
	  */
	def toJava = java.time.YearMonth.of(year.value, month.value)
	
	/**
	  * Separates this month to weeks
	  * @param w Week calendar system to apply
	  * @return A vector that contains all weeks in this month, first and last week may contain less than 7
	  *         days.
	  */
	def weeks(implicit w: WeekDays) = {
		import TimeExtensions._
		
		val d = dates
		val firstDay = w.first
		val weekLength = w.values.size
		
		val firstCompleteWeekStartIndex = d.iterator.indexWhere { _.weekDay == firstDay }
		// The first week and the last week may be incomplete
		val firstWeek = {
			if (firstCompleteWeekStartIndex == 0)
				None
			else
				Some(DateRange.exclusive(d.head, d(firstCompleteWeekStartIndex)))
		}
		val followingWeeks = Iterator
			.iterate(Pair(firstCompleteWeekStartIndex,
				firstCompleteWeekStartIndex + weekLength - 1)) { _.map { _ + weekLength }
			}
			.takeWhile { _.first < d.size }
			.map { _.map { i => d(i min (d.size - 1)) } }
			.map { p => DateRange.inclusive(p.first, p.last) }
		
		(firstWeek.iterator ++ followingWeeks).toVector
	}
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	override def name = month.name
	override def length = month.lengthAt(year)
	
	override def toString = s"${ month.value.toString.prependTo(2, '0') }/${ year.value }"
	override def toValue: Value = new Value(Some(this), YearMonthType)
	
	override def +(other: Int) = {
		var year = this.year
		var months = month.value + other
		
		while (months < 1) {
			year = year.previous
			months += 12
		}
		while (months > 12) {
			year = year.next
			months -= 12
		}
		
		YearMonth(year, Month(months))
	}
	override def -(other: Int) = this + (-other)
	override def compareTo(o: YearMonth) = {
		if (year == o.year)
			month.compareTo(o.month)
		else
			year.compareTo(o.year)
	}
	
	override def next(direction: Sign) = month.nextOption(direction) match {
		case Some(nextMonth) => copy(month = nextMonth)
		case None =>
			direction match {
				case Positive => YearMonth(year.next, January)
				case Negative => YearMonth(year.previous, December)
			}
	}
	override def is(extreme: Extreme) = false
	
	
	// OTHER    --------------------------
	
	/**
	  * @param day Targeted day of month
	  * @return That day in this month
	  */
	@throws[DateTimeException]("If the specified day is out of range")
	def apply(day: Int) = LocalDate.of(year.value, month.value, day)
	/**
	  * @param dayOfMonth Targeted day of month
	  * @return That day on this year month
	  * @throws DateTimeException If specified day is out of range
	  */
	@throws[DateTimeException]("If specified day is out of range")
	def /(dayOfMonth: Int) = apply(dayOfMonth)
}
