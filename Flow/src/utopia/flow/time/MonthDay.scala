package utopia.flow.time

import utopia.flow.operator.Steppable
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Month.{December, January}

import scala.language.implicitConversions

object MonthDay
{
	// TYPES    -------------------------
	
	/**
	  * A Java version of this class
	  */
	type JMonthDay = java.time.MonthDay
	
	
	// ATTRIBUTES   ---------------------
	
	/**
	 * The first month day in a year (assuming the Gregorian calendar)
	 */
	lazy val first = apply(January, 1)
	/**
	 * The last month day in a year (assuming the Gregorian calendar)
	 */
	lazy val last = apply(December, 31)
	
	
	// IMPLICIT -------------------------
	
	implicit def apply(monthDay: JMonthDay): MonthDay = apply(Month(monthDay.getMonthValue), monthDay.getDayOfMonth)
	implicit def toJava(monthDay: MonthDay): JMonthDay = monthDay.toJava
	
	
	// OTHER    -------------------------
	
	/**
	 * Creates a new month day
	 * @param month Targeted month
	 * @param day Targeted day of month
	 * @param assumeLeapYear Whether to assume a leap year (default = false)
	 * @return A new month day
	 */
	@throws[IndexOutOfBoundsException]("If 'day' is outside of the valid range for that month")
	def apply(month: Month, day: Int, assumeLeapYear: Boolean = false): MonthDay = {
		if (day < 1)
			throw new IndexOutOfBoundsException(s"$day is not a valid day of month")
		else if (day > month.lengthAt(assumeLeapYear).length) {
			// Case: Day of month is too large, but not on a leap year => Assumes that this is a leap year value
			if (!assumeLeapYear && month.isDifferentOnLeapYear && day == month.lengthAt(leapYear = true).length)
				new MonthDay(month, day, assumeLeapYear = true)
			else
				throw new IndexOutOfBoundsException(
					if (day > 31)
						s"$day is not a valid day of month"
					else
						s"$day is not a valid day of $month")
		}
		else
			new MonthDay(month, day, assumeLeapYear)
	}
	
	/**
	 * @param dayOfYear A day of year
	 * @return A month day matching that day-of-year
	 */
	def apply(dayOfYear: Int): MonthDay = apply(dayOfYear, leapYear = false)
	/**
	 * @param dayOfYear A day of year
	 * @param leapYear Whether targeting a leap year (default = false)
	 * @return A month day matching that day-of-year
	 */
	def apply(dayOfYear: Int, leapYear: Boolean) = {
		// If 'dayOfYear' is outside the targeted year's date range, won't consider the targeted year a leap year
		val shouldApplyLeapYear = leapYear && dayOfYear >= 1 && dayOfYear <= 366
		
		// Iterates months and days until a valid value is reached
		var days = dayOfYear
		var month: Month = January
		
		// Moves the day count to a positive value
		while (days < 1) {
			days += 365
		}
		// Moves the month forward until the date becomes valid
		while (days > month.lengthAt(shouldApplyLeapYear).length) {
			days -= month.lengthAt(shouldApplyLeapYear).length
			month = month.next
		}
		
		new MonthDay(month, days, shouldApplyLeapYear)
	}
}

/**
 * Represents a specific day of month, not specific to any year
 *
 * @author Mikko Hilpinen
 * @since 04.06.2025, v2.7
 */
case class MonthDay private(month: Month, day: Int, assumeLeapYear: Boolean)
	extends SelfComparable[MonthDay] with Combinable[Days, MonthDay] with Steppable[MonthDay]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return The quarter in which this day belongs
	  */
	def quarter = month.quarter
	
	/**
	  * @return The following month day
	  */
	def tomorrow = next(Positive)
	/**
	  * @return The previous month day
	  */
	def yesterday = next(Negative)
	
	/**
	 * @return Copy of this month day, which considers the current year a leap year
	 */
	def onLeapYear = copy(assumeLeapYear = true)
	/**
	 * @return Copy of this month day, considering the year not a leap year
	 */
	def onNormalYear = copy(assumeLeapYear = false)
	
	/**
	  * @return A Java version of this instance
	  */
	def toJava = java.time.MonthDay.of(month.value, day)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	override def next(direction: Sign): MonthDay = direction match {
		case Positive =>
			if (day == month.lengthAt(assumeLeapYear).length)
				copy(month = month.next, day = 1)
			else
				copy(day = day + 1)
		
		case Negative =>
			if (day <= 1) {
				val newMonth = month.previous
				copy(month = newMonth, day = newMonth.lengthAt(assumeLeapYear).length)
			}
			else
				copy(day = day - 1)
	}
	override def is(extreme: Extreme): Boolean = {
		if (month.is(extreme))
			extreme match {
				case Min => day <= 1
				case Max => day >= month.lengthAt(assumeLeapYear).length
			}
		else
			false
	}
	
	override def +(other: Days): MonthDay = this + other.length
	override def compareTo(o: MonthDay) = {
		if (month == o.month)
			day.compareTo(o.day)
		else
			month.compareTo(o.month)
	}
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param daysAdvance Number of days to advance
	  * @return An advanced month day
	  */
	def +(daysAdvance: Int) = {
		var month = this.month
		var days = day + daysAdvance
		
		// Makes sure the day value stays within a valid range by adjusting the month value
		while (days < 1) {
			month = month.previous
			days += month.lengthAt(assumeLeapYear).length
		}
		while (days > month.lengthAt(assumeLeapYear).length) {
			days -= month.lengthAt(assumeLeapYear).length
			month = month.next
		}
		
		MonthDay(month, days)
	}
	/**
	  * @param days Number of days to move to the past
	  * @return A reduced month day
	  */
	def -(days: Int) = this + (-days)
	
	/**
	  * @param year Targeted year
	  * @return This day on that year
	  */
	def at(year: Year) = year(this)
	/**
	  * @param year Targeted year
	  * @return This day on that year
	  */
	def /(year: Year) = at(year)
	
	/**
	  * @param another Another month day (exclusive)
	  * @return A yearly date range that starts from this day and ends at the specified date
	  */
	def until(another: MonthDay) = YearlyDateRange.exclusive(this, another)
}
