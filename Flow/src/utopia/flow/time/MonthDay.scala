package utopia.flow.time

import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.time.Month.{December, January}

object MonthDay
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * The first month day in a year (assuming the Gregorian calendar)
	 */
	lazy val first = apply(January, 1)
	/**
	 * The last month day in a year (assuming the Gregorian calendar)
	 */
	lazy val last = apply(December, 31)
	
	
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
		else if (day > month.lengthAt(assumeLeapYear).length)
			throw new IndexOutOfBoundsException(
				if (day > 31)
					s"$day is not a valid day of month"
				else if (assumeLeapYear && month.isDifferentOnLeapYear)
					s"$day is not a valid day of $month (on a leap year)"
				else
					s"$day is not a valid day of $month")
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
	extends SelfComparable[MonthDay] with Combinable[Days, MonthDay]
{
	// COMPUTED ----------------------------
	
	/**
	 * @return Copy of this month day, which considers the current year a leap year
	 */
	def onLeapYear = copy(assumeLeapYear = true)
	/**
	 * @return Copy of this month day, considering the year not a leap year
	 */
	def onNormalYear = copy(assumeLeapYear = false)
	
	
	// IMPLEMENTED  ------------------------
	
	override def self = this
	
	override def +(other: Days) = {
		var month = this.month
		var days = day + other.length
		
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
	override def compareTo(o: MonthDay) = {
		if (month == o.month)
			day.compareTo(o.day)
		else
			month.compareTo(o.month)
	}
}
