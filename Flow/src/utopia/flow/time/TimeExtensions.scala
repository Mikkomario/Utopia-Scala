package utopia.flow.time

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.SelfComparable

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAmount
import java.util.concurrent.TimeUnit
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.math.Ordered.orderingToOrdered
import scala.util.Try

/**
  * This object contains some extensions for java's time classes
  * @author Mikko Hilpinen
  * @since 17.11.2018
  * */
object TimeExtensions
{
	/*
	private trait TimeComparable[-A <: Repr, +Repr] extends Any
	{
		// ABSTRACT --------------------------
		
		/**
		  * @return This instance
		  */
		def repr: Repr
		
		/**
		  * @param other Another time
		  * @return Whether this time comes before that time
		  */
		def <(other: A): Boolean
		/**
		  * @param other Another time
		  * @return Whether this time comes after that time
		  */
		def >(other: A): Boolean
		
		
		// OTHER    --------------------------
		
		/**
		  * @param other Another time
		  * @return Whether this time is equal to or comes before that time
		  */
		def <=(other: A) = !(this > other)
		/**
		  * @param other Another time
		  * @return Whether this time is equal to or comes after that time
		  */
		def >=(other: A) = !(this < other)
		
		/**
		  * @param other Another time
		  * @return The smaller of these two times
		  */
		def min(other: A) = if (this <= other) repr else other
		/**
		  * @param other Another time
		  * @return The larger of these two times
		  */
		def max(other: A) = if (this >= other) repr else other
	}*/
	
	implicit class ExtendedInstant(val i: Instant) extends AnyVal with SelfComparable[Instant]
	{
		// COMPUTED --------------------------
		
		/**
		  * The date time value of this instant in the local time zone
		  */
		def toLocalDateTime = i.atZone(ZoneId.systemDefault()).toLocalDateTime
		/**
		 * @return The date value of this instant in the local time zone
		 */
		def toLocalDate = toLocalDateTime.toLocalDate
		/**
		  * @return Time portion of this instant in local time zone
		  */
		def toLocalTime = toLocalDateTime.toLocalTime
		/**
		  * The date time value of this instant in the UTC 'zulu' time zone
		  */
		def toUTCDateTime = i.atZone(ZoneId.of("Z")).toLocalDateTime
		
		/**
		  * @return Whether this instant is in the past
		  */
		def isInPast = this < Now
		/**
		  * @return Whether this instant is currently in the future
		  */
		def isInFuture = this > Now
		
		
		// IMPLEMENTED  ----------------------
		
		override def self = i
		
		override def compareTo(o: Instant) = i.compareTo(o)
		
		
		// OTHER	--------------------------
		
		/**
		  * Converts this instant to a string using specified formatter. If the formatter doesn't support instant
		  * naturally, converts this instant to local date time before converting to string
		  * @param formatter A date time formatter
		  * @return Formatted string representation of this instant
		  */
		def toStringWith(formatter: DateTimeFormatter) =
			Try { formatter.format(i) }.getOrElse { formatter.format(toLocalDateTime) }
		
		/**
		  * An instant after the specified duration has passed from this instant
		  */
		def +(amount: TemporalAmount) =
		{
			amount match {
				case period: Period =>
					// Long time periods are a bit tricky because the actual length of traversed time depends
					// from the context (date + time zone + calendars etc.)
					// However, when the user asks Instant.now() + 3 months, we can estimate that he probably means
					// 3 months in the local time context
					if (period.getMonths != 0 || period.getYears != 0)
						toLocalDateTime.plus(period).toInstantInDefaultZone
					else
						i.plus(period)
				
				case _ => i.plus(amount)
			}
		}
		/**
		  * @param amount Amount of duration to move this instant
		  * @return An instant after specified duration has passed from this instant
		  */
		def +(amount: FiniteDuration) = i.plusNanos(amount.toNanos)
		
		/**
		  * An instant before the specified duration
		  */
		def -(amount: TemporalAmount) =
		{
			amount match {
				case period: Period =>
					// See + for explanation
					if (period.getMonths != 0 || period.getYears != 0)
						toLocalDateTime.minus(period).toInstantInDefaultZone
					else
						i.minus(period)
				
				case _ => i.minus(amount)
			}
		}
		/**
		  * @param amount Amount of time to subtract
		  * @return The instant before this instant by specified duration
		  */
		def -(amount: FiniteDuration) = i.minusNanos(amount.toNanos)
		/**
		  * Finds the difference (duration) between the two time instances
		  */
		def -(time: Instant) = Duration.between(time, i)
		
		/**
		  * @param other Another instant
		  * @return Whether these two instants are the same, milliseconds-wise
		  */
		def ~==(other: Instant) = i.toEpochMilli == other.toEpochMilli
		
		/**
		  * @param other Another instant
		  * @return Time period from this instant to the other
		  */
		def until(other: Instant) = other - i
	}
	
	implicit class ExtendedLocalDateTime(val d: LocalDateTime) extends AnyVal with SelfComparable[LocalDateTime]
	{
		// COMPUTED	------------------------------
		
		/**
		  * @return Converts this date time to an instant. Expects this date time to be in system default zone
		  */
		def toInstantInDefaultZone = d.toInstant(ZoneId.systemDefault().getRules.getOffset(d))
		
		
		// IMPLEMENTED  --------------------------
		
		override def self = d
		
		override def compareTo(o: LocalDateTime) = d.compareTo(o)
		
		
		// OTHER	------------------------------
		
		/**
		  * @param length A length of time
		  * @return A time 'length' after this one
		  */
		def +(length: TemporalAmount) = d.plus(length)
		/**
		  * @param length A length of time
		  * @return A time 'length' before this one
		  */
		def -(length: TemporalAmount) = d.minus(length)
		
		/**
		  * @param days A period of days
		  * @return A date time 'days' days after this one
		  */
		def +(days: Days) = d.plusDays(days.length)
		/**
		  * @param days A period of days
		  * @return A date time 'days' days before this one
		  */
		def -(days: Days) = d.minusDays(days.length)
		
		/**
		  * @param other Another local date time
		  * @return The duration between these two times
		  */
		def -(other: LocalDateTime) = Duration.between(other, d)
		/**
		  * @param other Another local date time
		  * @return The duration from this time point to the second time point
		  */
		def until(other: LocalDateTime) = other - d
	}
	
	implicit class ExtendedDuration(val d: Duration) extends AnyVal with SelfComparable[Duration]
	{
		override def self = d
		
		override def compareTo(o: Duration) = d.compareTo(o)
	}
	
	implicit class ExtendedScalaDuration(val d: duration.Duration) extends AnyVal
	{
		// COMPUTED -------------------------------
		
		/**
		  * @return Whether this duration is an infinite duration
		  */
		def isInfinite = !d.isFinite
		
		/**
		  * @return A finite version of this duration. None for infinite durations.
		  */
		def finite = if (d.isFinite) Some(FiniteDuration(d.length, d.unit)) else None
		
		/**
		  * @return Describes this duration in a suitable unit and precision
		  */
		def description = finite match
		{
			case Some(d) =>
				val seconds = d.toPreciseSeconds
				if (seconds.abs < 0.1) {
					val millis = d.toPreciseMillis
					if (millis < 0.1)
						s"${ d.toNanos } nanos"
					else if (millis < 1)
						f"$millis%1.2f millis"
					else
						s"${ millis.toInt.toString } millis"
				}
				else if (seconds.abs >= 120) {
					val hoursPart = (seconds / 3600).toInt
					val minutesPart = ((seconds % 3600) / 60).toInt
					val secondsPart = (seconds % 60).toInt
					
					if (hoursPart.abs > 72) {
						if (hoursPart.abs > 504)
						{
							val weeks = d.toPreciseWeeks
							f"$weeks%1.2f weeks"
						}
						else {
							val hours = d.toPreciseHours
							val daysPart = (hours / 24.0).toInt
							val dayHoursPart = hours % 24
							if (dayHoursPart >= 23.995)
								s"${ daysPart + 1 } days"
							else if (dayHoursPart <= -23.995)
								s"${ daysPart - 1 } days"
							else if (dayHoursPart.abs >= 0.005)
								f"$daysPart days $dayHoursPart%1.2f hours"
							else
								s"$daysPart days"
						}
					}
					else if (hoursPart != 0)
						s"$hoursPart h $minutesPart min"
					else
						s"$minutesPart min $secondsPart s"
				}
				else
					f"$seconds%1.2f seconds"
			case None =>
				if (d < duration.Duration.Zero)
					"negative infinity"
				else
					"infinite"
		}
		
		
		// OTHER    ----------------------------------
		
		/**
		  * @param threshold A time threshold
		  * @return Whether this duration has passed since that time threshold
		  */
		def hasPassedSince(threshold: Instant) = finite match
		{
			case Some(d) => Now >= threshold + d
			case None => d < duration.Duration.Zero
		}
		/**
		  * @param threshold A time threshold
		  * @return Whether this duration has passed since that time threshold
		  */
		def hasPassedSince(threshold: LocalDateTime) = finite match
		{
			case Some(d) => Now.toLocalDateTime >= threshold + d
			case None => d < duration.Duration.Zero
		}
	}
	
	implicit class ExtendedFiniteDuration(val d: FiniteDuration) extends AnyVal
	{
		/**
		  * @return This duration in milliseconds, but with double precision (converted from nanoseconds)
		  */
		def toPreciseMillis = d.toNanos / 1000000.0
		/**
		  * @return This duration in seconds, but with double precision (converted from nanoseconds)
		  */
		def toPreciseSeconds = toPreciseMillis / 1000.0
		/**
		  * @return This duration in minutes, but with double precision (converted from nanoseconds)
		  */
		def toPreciseMinutes = toPreciseSeconds / 60.0
		/**
		  * @return This duration in hours, but with double precision (converted from nanoseconds)
		  */
		def toPreciseHours = toPreciseMinutes / 60.0
		/**
		  * @return This duration in days, but with double precision (converted from nanoseconds)
		  */
		def toPreciseDays = toPreciseHours / 24.0
		/**
		  * @return This duration in weeks, but with double precision (converted from nanoseconds)
		  */
		def toPreciseWeeks = toPreciseDays / 7.0
		
		/**
		  * @param instant Origin instant
		  * @return An instant 'this' before the origin instant
		  */
		def before(instant: Instant) = instant - d
		/**
		  * @param instant Origin instant
		  * @return An instant 'this' after the origin instant
		  */
		def after(instant: Instant) = instant + d
		
		/**
		  * @param date Origin date time
		  * @return A date time 'this' before the origin date time
		  */
		def before(date: LocalDateTime) = date - d
		/**
		  * @param date Origin date time
		  * @return A date time 'this' after the origin date time
		  */
		def after(date: LocalDateTime) = date + d
	}
	
	implicit class ExtendedLocalDate(val d: LocalDate) extends AnyVal with SelfComparable[LocalDate]
	{
		// COMPUTED	-------------------------
		
		/**
		  * @return Year of this date
		  */
		def year = Year.of(d.getYear)
		/**
		  * @return Month of this date
		  */
		def month = d.getMonth
		/**
		  * @return The day of month of this date [1, 31]
		  */
		def dayOfMonth = d.getDayOfMonth
		/**
		 * @return The month of year of this date [1, 12]
		 */
		def monthOfYear = d.getMonthValue
		/**
		  * @return Week day of this date
		  */
		def weekDay: WeekDay = d.getDayOfWeek
		/**
		  * @return The month day of this date (excludes year information)
		  */
		def monthDay = month(dayOfMonth)
		/**
		  * @return Year + month of this date
		  */
		def yearMonth = year + month
		
		/**
		  * @return The following date
		  */
		def next = d.plusDays(1)
		/**
		  * @return The previous date
		  */
		def previous = d.minusDays(1)
		/**
		  * @return A date previous to this day
		  */
		def yesterday = d.minusDays(1)
		/**
		  * @return A date after this day
		  */
		def tomorrow = d.plusDays(1)
		
		/**
		  * @return Converts this date to an instant. The time is expected to be 00:00. Expects this date time
		  *         to be in system default zone
		  */
		def toInstantInDefaultZone = d.atStartOfDay(ZoneId.systemDefault()).toInstant
		
		
		// IMPLEMENTED  --------------------
		
		override def self = d
		
		override def compareTo(o: LocalDate) = d.compareTo(o)
		
		
		// OTHER	------------------------
		
		/**
		  * Adds a number of days to this date
		  * @param timePeriod A time period to add
		  * @return A modified copy of this date
		  */
		def +(timePeriod: Period) = d.plus(timePeriod)
		def +(days: Days) = d.plusDays(days.length)
		/**
		  * Adds a time duration to this date
		  * @param duration A time duration
		  * @return a datetime based on this date and added time duration
		  */
		def +(duration: Duration) = d.atStartOfDay().plus(duration)
		/**
		  * Adds a time duration to this date
		  * @param duration A time duration
		  * @return a datetime based on this date and added time duration
		  */
		def +(duration: FiniteDuration): LocalDateTime = this + (duration: Duration)
		/**
		  * @param time A time element
		  * @return This date at specified time
		  */
		def +(time: LocalTime) = d.atTime(time)
		/**
		  * @param days Number of days to add
		  * @return This date advanced by specified number of days
		  */
		def +(days: Int) = d.plusDays(days)
		
		/**
		  * Subtracts a number of days to this date. Eg. 2.1.2001 + 3 hours would be 2.1.2001 03:00.
		  * @param timePeriod A time period to subtract
		  * @return A modified copy of this date
		  */
		def -(timePeriod: Period) = d.minus(timePeriod)
		def -(days: Days) = d.minusDays(days.length)
		/**
		  * Subtracts a time duration to this date. Eg. 2.1.2001 - 3 hours would be 1.1.2001 21:00.
		  * @param duration A time duration
		  * @return a datetime based on this date and subtracted time duration
		  */
		def -(duration: Duration) = d.atStartOfDay().minus(duration)
		/**
		  * @param other Another date
		  * @return Time period between these two dates (from specified date to this date)
		  */
		def -(other: LocalDate) = Days((d.toEpochDay - other.toEpochDay).toInt)
		/**
		  * Subtracts a time duration to this date. Eg. 2.1.2001 - 3 hours would be 1.1.2001 21:00.
		  * @param duration A time duration
		  * @return a datetime based on this date and subtracted time duration
		  */
		def -(duration: FiniteDuration): LocalDateTime = this - (duration: Duration)
		/**
		  * @param time A time element
		  * @return A date time received from subtracting specified time amount from this date's 00:00
		  */
		def -(time: LocalTime): LocalDateTime = this - time.toDuration
		/**
		  * @param days Number of days to subtract
		  * @return This date preceded by specified number of days
		  */
		def -(days: Int) = d.minusDays(days)
		
		/**
		  * @param weekDay     A week day
		  * @param includeSelf Whether this date should be returned in case it has that week day (default = false,
		  *                    which would return a day exactly one week from this day in case this day's week day
		  *                    matches searched week day)
		  * @return Next 'weekDay' after this day. May be this day if 'includeSelf' is set to true.
		  */
		def next(weekDay: WeekDay, includeSelf: Boolean = false) =
			Iterator.iterate(if (includeSelf) d else tomorrow) { _.tomorrow }.find { _.weekDay == weekDay }.get
		/**
		  * @param weekDay     A week day
		  * @param includeSelf Whether this date should be returned in case it has that week day (default = false,
		  *                    which would return a day exactly one week before this day in case this day's week day
		  *                    matches searched week day)
		  * @return Last 'weekDay' before this day. May be this day if 'includeSelf' is set to true.
		  */
		def previous(weekDay: WeekDay, includeSelf: Boolean = false) =
			Iterator.iterate(if (includeSelf) d else yesterday) { _.yesterday }.find { _.weekDay == weekDay }.get
		
		/**
		  * @param other Another date
		  * @return Dates between these two dates (including both dates)
		  */
		def to(other: LocalDate) = DateRange.inclusive(d, other)
		
		/**
		  * @param other Another date
		  * @return Dates between these two dates. Includes this date but excludes the other.
		  */
		def toExclusive(other: LocalDate) = DateRange.exclusive(d, other)
	}
	
	implicit class ExtendedLocalTime(val t: LocalTime) extends AnyVal with SelfComparable[LocalTime]
	{
		// COMPUTED	----------------------
		
		/**
		  * @return A duration based on this time element (from the beginning of day)
		  */
		def toDuration = t.toNanoOfDay.nanos
		
		
		// IMPLEMENTED  ------------------
		
		override def self = t
		
		override def compareTo(o: LocalTime) = t.compareTo(o)
		
		
		// OTHER	----------------------
		
		/**
		  * @param other Another time
		  * @return Duration from 'other' to this. Negative if 'other' comes after this.
		  */
		def -(other: LocalTime) = toDuration - other.toDuration
		/**
		  * @param duration a duration
		  * @return A copy of this time shifted by 'duration' to past
		  */
		def -(duration: Duration) = t.minus(duration)
		
		/**
		  * @param duration a duration
		  * @return A copy of this time shifted by 'duration' to future
		  */
		def +(duration: Duration) = t.plus(duration)
	}
	
	implicit class ExtendedMonth(val m: Month) extends AnyVal with SelfComparable[Month]
	{
		// COMPUTED ----------------------
		
		/**
		  * Alias for .getValue
		  * @return The integer value of this month [1,12]
		  */
		def value = m.getValue
		
		/**
		 * @return First day of this month
		 */
		def firstDay = apply(1)
		/**
		 * @return The quarter where this month belongs
		 */
		def quarter = Quarter.containing(m)
		
		/**
		  * @return The month after this one
		  */
		def next = this + 1
		/**
		  * @return The month previous to this one
		  */
		def previous = this - 1
		
		
		// IMPLEMENTED  ------------------
		
		override def self = m
		
		override def compareTo(o: Month) = m.compareTo(o)
		
		
		// OTHER    ----------------------
		
		/**
		  * @param months Number of months to add
		  * @return This month advanced by that many months (rolls over at December/January)
		  */
		def +(months: Int) = m.plus(months)
		/**
		  * @param months Number of months to subtract
		  * @return This month preceded by that many months (rolls over at December/January)
		  */
		def -(months: Int) = m.minus(months)
		
		/**
		  * @param day Targeted day of this month
		  * @return Specified day on this month [1, 31]
		  * @throws DateTimeException If specified day is out of range
		  */
		@throws[DateTimeException]("Specified day is out of range")
		def apply(day: Int) = MonthDay.of(m, day)
		
		/**
		  * @param year Target year
		  * @return This month at that year
		  */
		def atYear(year: Int) = YearMonth.of(year, m)
		/**
		  * @param year Target year
		  * @return This month at that year
		  */
		def at(year: Year) = year + m
		/**
		  * @param year Target year
		  * @return This month at that year
		  */
		def /(year: Year) = at(year)
		/**
		  * @param year Target year
		  * @return This month at that year
		  */
		def +(year: Year) = at(year)
	}
	
	implicit class ExtendedYear(val y: Year) extends AnyVal with SelfComparable[Year]
	{
		// COMPUTED -------------------------
		
		/**
		  * Alias for .getValue
		  * @return Integer value of this year
		  */
		def value = y.getValue
		
		/**
		  * @return The year following this one
		  */
		def next = this + 1
		/**
		  * @return The year previous to this one
		  */
		def previous = this - 1
		
		/**
		  * @return The first day of this year
		  */
		def firstDay: LocalDate = y(1)(1)
		/**
		  * @return The last day of this year
		  */
		def lastDay: LocalDate = y(12).lastDay
		
		/**
		  * @return January (1) of this year
		  */
		def january = apply(Month.JANUARY)
		/**
		  * @return February (2) of this year
		  */
		def february = apply(Month.FEBRUARY)
		/**
		  * @return March (3) of this year
		  */
		def march = apply(Month.MARCH)
		/**
		  * @return April (4) of this year
		  */
		def april = apply(Month.APRIL)
		/**
		  * @return May (5) of this year
		  */
		def may = apply(Month.MAY)
		/**
		  * @return June (6) of this year
		  */
		def june = apply(Month.JUNE)
		/**
		  * @return July (7) of this year
		  */
		def july = apply(Month.JULY)
		/**
		  * @return August (8) of this year
		  */
		def august = apply(Month.AUGUST)
		/**
		  * @return September (9) of this year
		  */
		def september = apply(Month.SEPTEMBER)
		/**
		  * @return October (10) of this year
		  */
		def october = apply(Month.OCTOBER)
		/**
		  * @return November (11) of this year
		  */
		def november = apply(Month.NOVEMBER)
		/**
		  * @return December (12) of this year
		  */
		def december = apply(Month.DECEMBER)
		
		
		// IMPLEMENTED  ---------------------
		
		override def self = y
		
		override def compareTo(o: Year) = y.compareTo(o)
		
		
		// OTHER    -------------------------
		
		/**
		  * @param years Number of years to add
		  * @return That many years after this year
		  */
		def +(years: Int) = y.plusYears(years)
		/**
		  * @param years Years to subtract
		  * @return That many years before this year
		  */
		def -(years: Int) = y.minusYears(years)
		
		/**
		  * Adds month information to this year
		  * @param month Targeted month
		  * @return A monthYear based on this year and specified month
		  */
		def +(month: Month) = YearMonth.of(y.getValue, month)
		/**
		  * Adds month information (same as this + month)
		  * @param month targeted month
		  * @return targeted month on this year
		  */
		def /(month: Month) = this + month
		/**
		  * @param month targeted month
		  * @return targeted month on this year
		  */
		def /(month: Int) = apply(month)
		
		/**
		  * @param monthDay A month day
		  * @return That month day during this year
		  */
		def /(monthDay: MonthDay) = apply(monthDay)
		
		/**
		  * @param range A range of dates
		  * @return The portion of that range that overlaps with this year. 0-2 different ranges.
		  */
		def /(range: YearlyDateRange) = apply(range)
		
		/**
		  * @param month Targeted month
		  * @return Specified month on this year
		  */
		def apply(month: Month): YearMonth = this + month
		/**
		  * @param month Targeted month (number) [1, 12]
		  * @return Targeted month on this year
		  * @throws DateTimeException If specified month is out of range
		  */
		@throws[DateTimeException]("Specified month is out of range")
		def apply(month: Int): YearMonth = YearMonth.of(y.getValue, month)
		
		/**
		  * @param day Target month day
		  * @return Specified month day on this year
		  */
		def apply(day: MonthDay): LocalDate = y.atMonthDay(day)
		
		/**
		  * @param range A range of dates
		  * @return The portion of that range that overlaps with this year. 0-2 different ranges.
		  */
		def apply(range: YearlyDateRange): Vector[DateRange] = range.at(y)
	}
	
	implicit class ExtendedMonthDay(val md: MonthDay) extends AnyVal with SelfComparable[MonthDay]
	{
		// COMPUTED ----------------------
		
		/**
		 * @return Quarter to which this day belongs
		 */
		def quarter = month.quarter
		/**
		  * @return Month portion of this month day
		  */
		def month = md.getMonth
		/**
		  * @return Day portion of this month day
		  */
		def day = md.getDayOfMonth
		
		
		// IMPLEMENTED-------------------
		
		override def self = md
		
		override def compareTo(o: MonthDay) = md.compareTo(o)
		
		
		// OTHER    ---------------------
		
		/**
		  * @param year Target year
		  * @return This month day at that year
		  */
		def at(year: Year) = md.atYear(year.getValue)
		/**
		  * @param year Target year
		  * @return This month day at that year
		  */
		def /(year: Year) = at(year)
		/**
		  * @param year Target year
		  * @return This month day at that year
		  */
		def /(year: Int) = md.atYear(year)
		
		/**
		  * @param another Another month day (exclusive)
		  * @return A yearly date range that starts from this day and ends at the specified date
		  */
		def until(another: MonthDay) = YearlyDateRange.exclusive(md, another)
	}
	
	implicit class ExtendedYearMonth(val ym: YearMonth) extends AnyVal with SelfComparable[YearMonth]
	{
		// COMPUTED	----------------------
		
		/**
		  * @return Year portion of this year month
		  */
		def year = Year.of(ym.getYear)
		/**
		  * @return Month part of this year month
		  */
		def month = ym.getMonth
		
		/**
		  * @return Number of days within this month
		  */
		def length = ym.lengthOfMonth()
		
		/**
		  * @return Dates in this month as a date range
		  */
		def dates = firstDay to lastDay
		
		/**
		  * @return The first day of this month
		  */
		def firstDay = ym.atDay(1)
		/**
		  * @return The last day of this month
		  */
		def lastDay = apply(length)
		
		/**
		  * @return The year month previous to this one
		  */
		def previous = this - 1
		/**
		  * @return The year month following this one
		  */
		def next = this + 1
		
		/**
		  * Separates this month to weeks
		  * @param w Week calendar system to apply
		  * @return A vector that contains all weeks in this month, first and last week may contain less than 7
		  *         days.
		  */
		def weeks(implicit w: WeekDays) = {
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
		
		
		// IMPLEMENTED--------------------
		
		override def self = ym
		
		override def compareTo(o: YearMonth) = ym.compareTo(o)
		
		
		// OTHER	----------------------
		
		/**
		  * Adjusts this month by specified amount
		  * @param monthAdjust Adjustment to month count
		  * @return Adjusted month
		  */
		def +(monthAdjust: Int) = ym.plusMonths(monthAdjust)
		/**
		  * Adjusts this month by specified amount
		  * @param monthAdjust Adjustment to month count
		  * @return Adjusted month
		  */
		def -(monthAdjust: Int) = ym.minusMonths(monthAdjust)
		
		/**
		  * @param dayNumber Targeted day number
		  * @return Targeted date [1, 31]
		  * @throws DateTimeException If specified day is out of range
		  */
		@throws[DateTimeException]("If specified day is out of range")
		def apply(dayNumber: Int) = LocalDate.of(ym.getYear, ym.getMonth, dayNumber)
		/**
		  * @param dayOfMonth Targeted day of month
		  * @return That day on this year month
		  * @throws DateTimeException If specified day is out of range
		  */
		@throws[DateTimeException]("If specified day is out of range")
		def /(dayOfMonth: Int) = apply(dayOfMonth)
	}
	
	implicit class ExtendedPeriod(val p: Period) extends AnyVal with SelfComparable[Period]
	{
		// COMPUTED ---------------------------
		
		/**
		  * @return An approximate duration based on this time period
		  */
		def toApproximateDuration = (((p.getYears * 12 + p.getMonths) * 30.44 + p.getDays) * 24).hours
		
		
		// IMPLEMENTED	-----------------------
		
		override def self = p
		
		// Uses some rounding in comparing (not exact with months vs days). 1 month is considered to be 30.44 days
		override def compareTo(o: Period) = ((p.getYears * 12 + p.getMonths) * 30.44 + p.getDays -
			((o.getYears * 12 + o.getMonths) * 30.44 + o.getDays)).toInt
		
		
		// OTHER    --------------------------
		
		def +(days: Days) = p.plusDays(days.length)
		def -(days: Days) = p.minusDays(days.length)
	}
	
	implicit class TimeNumber[T](val i: T) extends AnyVal
	{
		private def nanoPrecision(mod: Long)(implicit n: Numeric[T]) =
			FiniteDuration((n.toDouble(i) * mod).toLong, TimeUnit.NANOSECONDS)
		
		/**
		  * @param n implicit numeric
		  * @return This number amount of nano seconds
		  */
		def nanos(implicit n: Numeric[T]) = nanoPrecision(1)
		/**
		  * @param n implicit numeric
		  * @return This number amount of milli seconds (provides nano precision with doubles)
		  */
		def millis(implicit n: Numeric[T]) = nanoPrecision(1000000)
		/**
		  * @param n implicit numeric
		  * @return This number amount of seconds (provides nano precision with doubles)
		  */
		def seconds(implicit n: Numeric[T]) = nanoPrecision(1000000L * 1000)
		/**
		  * @param n implicit numeric
		  * @return This number amount of minutes (provides nano precision with doubles)
		  */
		def minutes(implicit n: Numeric[T]) = nanoPrecision(1000000L * 1000 * 60)
		/**
		  * @param n implicit numeric
		  * @return This number amount of hours (provides nano precision with doubles)
		  */
		def hours(implicit n: Numeric[T]) = nanoPrecision(1000000L * 1000 * 60 * 60)
	}
	
	implicit class LocalTimeNumber(val i: Int) extends AnyVal
	{
		/**
		  * @return This number as a local hours number
		  */
		def oClock = LocalTime.of(i, 0)
		
		/**
		  * Converts two integers into a local time
		  * @param minutes Minutes to add to this hour
		  * @return "This" hour at 'minutes' minutes.
		  *         E.g. 17::54 is interpreted as 17:54
		  */
		def ::(minutes: Int) = LocalTime.of(i, minutes)
		
		/*
		  * Combines this minutes value with hours in order to form a local time value
		  * @param hour Target hour
		  * @return A local time value that's "this" many minutes to / before 'hour' hour.
		  *         E.g. 6 to 12 would yield 11:54
		  */
		// Removed because of name conflicts
		// def to(hour: Int) = if (hour == 0) LocalTime.of(23, i) else LocalTime.of(hour - 1, i)
		/**
		  * Combines this minutes value with hours in order to form a local time value
		  * @param hour Target hour
		  * @return A local time value that's "this" many minutes past / after 'hour' hour.
		  *         E.g. 6 past 12 would yield 12:06
		  */
		def past(hour: Int) = if (hour == 24) LocalTime.of(0, i) else LocalTime.of(hour, i)
	}
	
	implicit class DayCount(val i: Int) extends AnyVal
	{
		// COMPUTED ----------------------
		
		/**
		  * @return Period of this many days
		  */
		def days = Days(i)
		/**
		  * @return Period of this many weeks
		  */
		def weeks = Days(i * 7)
		
		/**
		  * @return Period of this many months
		  */
		def months = Period.ofMonths(i)
		/**
		  * @return Period of this many years
		  */
		def years = Period.ofYears(i)
		
		/**
		  * @return This number as a month
		  * @throws DateTimeException If this number was not a valid month (1-12)
		  */
		@throws[DateTimeException]("this number was not a valid month (1-12)")
		def month = Month.of(i)
		/**
		  * @return This number as a year
		  */
		def year = Year.of(i)
		
		
		// OTHER    ----------------------
		
		/**
		  * @param month Target month
		  * @return This day at that month
		  */
		def of(month: Month) = month(i)
		/**
		  * @param month Target month
		  * @return This day at that month
		  */
		def dayOf(month: Int) = MonthDay.of(month, i)
		
		/**
		  * @param year Targeted year
		  * @return This month at that year
		  */
		def of(year: Year) = year(i)
		/**
		  * @param year Targeted year
		  * @return This month at that year
		  */
		def monthOf(year: Int) = YearMonth.of(year, i)
		
		/**
		  * @param month Targeted month
		  * @return This day at that month
		  */
		def /(month: Month) = month(i)
		/**
		  * @param year Targeted year
		  * @return This month at that year
		  */
		def /(year: Year) = year(i)
	}
	
	/**
	  * Converts a java duration to a scala duration
	  */
	implicit def javaDurationToScalaDuration(duration: java.time.Duration): FiniteDuration =
		FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS)
	implicit def javaDurationToExtendedScalaDuration(duration: java.time.Duration): ExtendedScalaDuration =
		javaDurationToScalaDuration(duration)
	implicit def javaDurationToExtendedFiniteDuration(duration: java.time.Duration): ExtendedFiniteDuration =
		javaDurationToScalaDuration(duration)
	/**
	  * Converts a java duration option to scala duration
	  */
	implicit def javaDurationOptionToScalaDuration(duration: Option[java.time.Duration]): scala.concurrent.duration.Duration =
		duration.map(javaDurationToScalaDuration).getOrElse(scala.concurrent.duration.Duration.Inf)
	/**
	  * Converts a finite scala duration to a java duration
	  */
	implicit def scalaDurationToJavaDuration(duration: FiniteDuration): Duration =
		java.time.Duration.ofNanos(duration.toNanos)
	
	/*
	  * Converts a time period (days + months + years)
	  */
		/*
	implicit def periodToDuration(period: Period): duration.Duration =
	{
		// NB: Not exact! Expects one year to be 12 months and one month to be 30.44 days
		val days = (period.getYears * 12 + period.getMonths) * 30.44 + period.getDays
		(days * 24).hours
	}*/
}
