package utopia.flow.time

import utopia.flow.time.TimeExtensions._

import java.time.{LocalDate, MonthDay, Year}

object YearlyDateRange
{
	// OTHER    ------------------------------
	
	/**
	  * @param start The first date included in this range
	  * @param end The exclusive end date of this range
	  * @return A new range that repeats every year
	  */
	def exclusive(start: MonthDay, end: MonthDay) = apply(start, end)
}

/**
  * Represents a time period that can be repeated every year. For example: 1st of June to 3rd (middle of year) of
  * September or 5th of December to 13th of March (beginning & end of year).
  * @author Mikko Hilpinen
  * @since 11.1.2021, v1.9
  */
case class YearlyDateRange(start: MonthDay, end: MonthDay)
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * Whether this range contains all dates within a year
	  */
	val spansFullYear = end == start
	private val isSingleRangePerYear = end >= start
	
	
	// OTHER    ----------------------------
	
	/**
	  * Checks whether this day range contains specified date
	  * @param day month + day
	  * @return Whether this day range contains that month date
	  */
	def contains(day: MonthDay) = spansFullYear ||
		(if (isSingleRangePerYear) day < end && day >= start else day >= start || day < end)
	
	/**
	  * @param date A date
	  * @return Whether this date range contains the specified date (during that year)
	  */
	def contains(date: LocalDate) = spansFullYear || at(date.year).exists { _.contains(date) }
	
	/**
	  * @param year Target year
	  * @return This date range during that year. 0-2 separate ranges. 0 ranges if this range is empty.
	  *         If this range can be expressed as a single complete range of days during that year, returns 1 range.
	  *         Otherwise returns 2 ranges (one for the beginning of the year and one for the end of the year).
	  */
	def at(year: Year): Vector[DateRange] =
	{
		// Case: Empty range => returns empty vector
		if (spansFullYear)
			Vector(year.firstDay to year.lastDay)
		// Case: End of year is not passed => returns a single complete range
		else if (isSingleRangePerYear)
			Vector(year(start) toExclusive year(end))
		// Case: Range passes end of year => returns start of year segment and the end of year segment separately
		else
		{
			Vector(
				year.firstDay toExclusive year(end),
				year(start) to year.lastDay
			)
		}
	}
	
	/**
	  * @param year Target year
	  * @return This date range during that year. 0-2 separate ranges. 0 ranges if this range is empty.
	  *         If this range can be expressed as a single complete range of days during that year, returns 1 range.
	  *         Otherwise returns 2 ranges (one for the beginning of the year and one for the end of the year).
	  */
	def atYear(year: Int) = at(Year.of(year))
	
	/**
	  * @param year Target year
	  * @return This date range during that year. 0-2 separate ranges. 0 ranges if this range is empty.
	  *         If this range can be expressed as a single complete range of days during that year, returns 1 range.
	  *         Otherwise returns 2 ranges (one for the beginning of the year and one for the end of the year).
	  */
	def /(year: Year) = at(year)
}
