package utopia.flow.util

import java.time.chrono.ChronoLocalDate
import java.time.format.DateTimeFormatter

import scala.language.implicitConversions
import java.time.{Duration, Instant, LocalDate, LocalDateTime, LocalTime, Month, Period, Year, YearMonth, ZoneId}
import java.time.temporal.TemporalAmount

import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit

import scala.collection.immutable.VectorBuilder
import scala.util.Try
import utopia.flow.util.RichComparable._
import utopia.flow.util.WeekDay.Monday

/**
* This object contains some extensions for java's time classes
* @author Mikko Hilpinen
* @since 17.11.2018
**/
object TimeExtensions
{
	implicit class ExtendedInstant(val i: Instant) extends AnyVal
	{
		// OTHER	--------------------------
		
		/**
		 * Converts this instant to a string using specified formatter. If the formatter doesn't support instant
		 * naturally, converts this instant to local date time before converting to string
		 * @param formatter A date time formatter
		 * @return Formatted string representation of this instant
		 */
		def toStringWith(formatter: DateTimeFormatter) = Try { formatter.format(i) }.getOrElse { formatter.format(toLocalDateTime) }
		
	    /**
	     * The date time value of this instant in the local time zone
	     */
	    def toLocalDateTime = i.atZone(ZoneId.systemDefault()).toLocalDateTime
	    
	    /**
	     * The date time value of this instant in the UTC 'zulu' time zone
	     */
	    def toUTCDateTime = i.atZone(ZoneId.of("Z")).toLocalDateTime
	    
	    /**
	     * An instant after the specified duration has passed from this instant
	     */
	    def +(amount: TemporalAmount) =
		{
			amount match
			{
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
		def +(amount: duration.Duration) = i.plusNanos(amount.toNanos)
	    
	    /**
	     * An instant before the specified duration
	     */
	    def -(amount: TemporalAmount) =
		{
			amount match
			{
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
		def -(amount: duration.Duration) = i.minusNanos(amount.toNanos)
		
	    /**
	     * Finds the difference (duration) between the two time instances
	     */
	    def -(time: Instant) = Duration.between(time, i)
	    
	    /**
	     * Checks whether this instant comes before the specified instant
	     */
	    def <(other: Instant) = i.isBefore(other)
	    
	    /**
	     * Checks whether this instant comes after the specified instant
	     */
	    def >(other: Instant) = i.isAfter(other)
		
		/**
		  * @param other Another instant
		  * @return Whether this instant is before or equal to the other instant
		  */
		def <=(other: Instant) = !(this > other)
		
		/**
		  * @param other Another instant
		  * @return Whether this instant is equal or larger to the other instant
		  */
		def >=(other: Instant) = !(this < other)
		
		/**
		  * @param other Another instant
		  * @return Whether these two instants are the same, milliseconds-wise
		  */
		def ~==(other: Instant) = i.toEpochMilli == other.toEpochMilli
	}
	
	implicit class ExtendedLocalDateTime(val d: LocalDateTime) extends RichComparable[LocalDateTime]
	{
		// IMPLEMENTED	--------------------------
		
		override def compareTo(o: LocalDateTime) = d.toLocalDate.compareOr(o.toLocalDate) { d.toLocalTime.compareTo(o.toLocalTime) }
		
		
		// COMPUTED	------------------------------
		
		/**
		 * @return Converts this date time to an instant. Expects this date time to be in system default zone
		 */
		def toInstantInDefaultZone = d.toInstant(ZoneId.systemDefault().getRules.getOffset(d))
		
		
		// OTHER	------------------------------
		
		/**
		  * @param period A period of time
		  * @return Date time 'period' into future from this datetime
		  */
		def +(period: Period) = d.plus(period)
		
		/**
		  * @param period A period of time
		  * @return Date time 'period' into the past from this datetime
		  */
		def -(period: Period) = d.minus(period)
	}
	
	implicit class ExtendedDuration(val d: Duration) extends AnyVal
	{
	    /**
	     * This duration as milliseconds, but with double precision
	     */
	    def toPreciseMillis = d.toNanos / 1000000.0
		
		/**
		  * @return This duration in seconds, but with double precision
		  */
		def toPreciseSeconds = toPreciseMillis / 1000
		
		/**
		  * @return Describes this duration in a suitable unit and precision
		  */
		def description = javaDurationToScalaDuration(d).description
	}
	
	implicit class ExtendedScalaDuration(val d: duration.Duration) extends AnyVal
	{
		/**
		  * @return A finite version of this duration. None for infinite durations.
		  */
		def finite = if (d.isFinite) Some(FiniteDuration(d.length, d.unit)) else None
		
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
		  * @return Describes this duration in a suitable unit and precision
		  */
		def description =
		{
			val seconds = toPreciseSeconds
			if (seconds.abs < 0.1)
			{
				val millis = toPreciseMillis
				if (millis < 0.1)
					s"${d.toNanos} nanos"
				else if (millis < 1)
					f"$millis%1.2f millis"
				else
					s"${millis.toInt.toString} millis"
			}
			else if (seconds.abs >= 120)
			{
				val hoursPart = (seconds / 3600).toInt
				val minutesPart = ((seconds % 3600) / 60).toInt
				val secondsPart = (seconds % 60).toInt
				
				if (hoursPart.abs > 72)
				{
					if (hoursPart.abs > 504)
						f"$toPreciseWeeks%1.2f weeks"
					else
					{
						val hours = toPreciseHours
						val daysPart = (hours / 24.0).toInt
						val dayHoursPart = hours % 24
						if (dayHoursPart >= 23.995)
							s"${daysPart + 1} days"
						else if (dayHoursPart <= -23.995)
							s"${daysPart - 1} days"
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
		}
	}
	
	implicit class ExtendedLocalDate(val d: LocalDate) extends AnyVal
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
		  * @return Week day of this date
		  */
		def weekDay: WeekDay = d.getDayOfWeek
		/**
		  * @return Year + month of this date
		  */
		def yearMonth = d.year + d.month
		
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
		
		
		// OTHER	------------------------
		
		/**
		 * Adds a number of days to this date
		 * @param timePeriod A time period to add
		 * @return A modified copy of this date
		 */
		def +(timePeriod: Period) = d.plus(timePeriod)
		
		/**
		 * Adds a time duration to this date
		 * @param duration A time duration
		 * @return a datetime based on this date and added time duration
		 */
		def +(duration: Duration) = d.atStartOfDay().plus(duration)
		
		/**
		  * @param time A time element
		  * @return This date at specified time
		  */
		def +(time: LocalTime) = d.atTime(time)
		
		/**
		 * Subtracts a number of days to this date. Eg. 2.1.2001 + 3 hours would be 2.1.2001 03:00.
		 * @param timePeriod A time period to subtract
		 * @return A modified copy of this date
		 */
		def -(timePeriod: Period) = d.minus(timePeriod)
		
		/**
		 * Subtracts a time duration to this date. Eg. 2.1.2001 - 3 hours would be 1.1.2001 21:00.
		 * @param duration A time duration
		 * @return a datetime based on this date and subtracted time duration
		 */
		def -(duration: Duration) = d.atStartOfDay().minus(duration)
		
		/**
		 * Adds a time duration to this date
		 * @param duration A time duration
		 * @return a datetime based on this date and added time duration
		 */
		def +(duration: FiniteDuration): LocalDateTime = this + (duration: Duration)
		
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
		 * @param other Another date
		 * @return Whether this date comes before specified date
		 */
		def <(other: ChronoLocalDate) = d.isBefore(other)
		
		/**
		 * @param other Another date
		 * @return Whether this date comes after specified date
		 */
		def >(other: ChronoLocalDate) = d.isAfter(other)
		
		/**
		 * @param other Another date
		 * @return Whether this date comes before or is equal to specified date
		 */
		def <=(other: ChronoLocalDate) = !d.isAfter(other)
		
		/**
		 * @param other Another date
		 * @return Whether this date comes after or is equal to specified date
		 */
		def >=(other: ChronoLocalDate) = !d.isBefore(other)
		
		/**
		  * @param weekDay A week day
		  * @param includeSelf Whether this date should be returned in case it has that week day (default = false,
		  *                    which would return a day exactly one week from this day in case this day's week day
		  *                    matches searched week day)
		  * @return Next 'weekDay' after this day. May be this day if 'includeSelf' is set to true.
		  */
		def next(weekDay: WeekDay, includeSelf: Boolean = false) =
		{
			val current = this.weekDay
			if (current == weekDay)
			{
				if (includeSelf)
					d
				else
					this + 1.weeks
			}
			else
				this + (weekDay - current)
		}
		
		/**
		  * @param weekDay A week day
		  * @param includeSelf Whether this date should be returned in case it has that week day (default = false,
		  *                    which would return a day exactly one week before this day in case this day's week day
		  *                    matches searched week day)
		  * @return Last 'weekDay' before this day. May be this day if 'includeSelf' is set to true.
		  */
		def previous(weekDay: WeekDay, includeSelf: Boolean = false) =
		{
			val current = this.weekDay
			if (current == weekDay)
			{
				if (includeSelf)
					d
				else
					this - 1.weeks
			}
			else
				this - (current - weekDay)
		}
	}
	
	implicit class ExtendedLocalTime(val t: LocalTime) extends AnyVal
	{
		// COMPUTED	----------------------
		
		/**
		  * @return A duration based on this time element (from the beginning of day)
		  */
		def toDuration = t.toNanoOfDay.nanos
		
		
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
		
		/**
		  * @param other Another time
		  * @return Whether this time comes before the other time
		  */
		def <(other: LocalTime) = t.isBefore(other)
		
		/**
		  * @param other Another time
		  * @return Whether this time comes after the other time
		  */
		def >(other: LocalTime) = t.isAfter(other)
		
		/**
		  * @param other Another time
		  * @return Whether this time comes before or at the same time as the other time
		  */
		def <=(other: LocalTime) = !t.isAfter(other)
		
		/**
		  * @param other Another time
		  * @return Whether this time comes before or at the same time as the other time
		  */
		def >=(other: LocalTime) = !t.isBefore(other)
	}
	
	implicit class ExtendedYear(val y: Year) extends AnyVal
	{
		/**
		  * Adds month information to this year
		  * @param month Targeted month
		  * @return A monthYear based on this year and specified month
		  */
		def +(month: Month) = YearMonth.of(y.getValue, month)
	}
	
	implicit class ExtendedYearMonth(val ym: YearMonth) extends AnyVal
	{
		// COMPUTED	----------------------
		
		/**
		  * @return Year portion of this year month
		  */
		def year = Year.of(ym.getYear)
		
		/**
		  * @return Dates in this month
		  */
		def dates = (1 to ym.lengthOfMonth()).map(apply)
		
		/**
		  * @return The year month previous to this one
		  */
		def previous = this - 1
		
		/**
		  * @return The year month following this one
		  */
		def next = this + 1
		
		
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
		  * @return Targeted date
		  */
		def apply(dayNumber: Int) = LocalDate.of(ym.getYear, ym.getMonth, dayNumber)
		
		/**
		  * Separates this month to weeks
		  * @param firstDayOfWeek The first day of a week (default = Monday)
		  * @return A vector that contains all weeks in this month, first and last week may contain less than 7
		  *         days.
		  */
		def weeks(firstDayOfWeek: WeekDay = Monday) =
		{
			val d = dates
			// Month may start at the middle of the week
			val incompleteStart = d.takeWhile { _.weekDay > firstDayOfWeek }.toVector
			
			val weeksBuffer = new VectorBuilder[Vector[LocalDate]]()
			if (incompleteStart.nonEmpty)
				weeksBuffer += incompleteStart
			
			var nextWeekStartIndex = incompleteStart.size
			while (nextWeekStartIndex < d.size)
			{
				weeksBuffer += d.slice(nextWeekStartIndex, nextWeekStartIndex + 7).toVector
				nextWeekStartIndex += 7
			}
			
			weeksBuffer.result()
		}
	}
	
	implicit class ExtendedPeriod(val p: Period) extends RichComparable[Period]
	{
		// IMPLEMENTED	-----------------------
		
		// Uses some rounding in comparing (not exact with months vs days). 1 month is considered to be 30.44 days
		override def compareTo(o: Period) = ((p.getYears * 12 + p.getMonths) * 30.44 + p.getDays -
			((o.getYears * 12 + o.getMonths) * 30.44 + o.getDays)).toInt
	}
	
	implicit class TimeNumber[T](val i: T) extends AnyVal
	{
		private def nanoPrecision(mod: Long)(implicit n: Numeric[T]) = FiniteDuration((n.toDouble(i) * mod).toLong, TimeUnit.NANOSECONDS)
		
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
	
	implicit class DayCount(val i: Int) extends AnyVal
	{
		/**
		 * @return Period of this many days
		 */
		def days = Period.ofDays(i)
		
		/**
		 * @return Period of this many weeks
		 */
		def weeks = Period.ofWeeks(i)
		
		/**
		 * @return Period of this many months
		 */
		def months = Period.ofMonths(i)
		
		/**
		 * @return Period of this many years
		 */
		def years = Period.ofYears(i)
	}
	
	/**
	 * Converts a java duration to a scala duration
	 */
	implicit def javaDurationToScalaDuration(duration: java.time.Duration): FiniteDuration =
	        FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS)
	
	/**
	 * Converts a java duration option to scala duration
	 */
	implicit def javaDurationOptionToScalaDuration(duration: Option[java.time.Duration]): scala.concurrent.duration.Duration =
	        duration.map(javaDurationToScalaDuration).getOrElse(scala.concurrent.duration.Duration.Inf)
	
	/**
	 * Converts a finite scala duration to a java duration
	 */
	implicit def scalaDurationToJavaDuration(duration: FiniteDuration): Duration = java.time.Duration.ofNanos(duration.toNanos)
	
	/**
	  * Converts a time period (days + months + years)
	  */
	implicit def periodToDuration(period: Period): duration.Duration =
	{
		// NB: Not exact! Expects one year to be 12 months and one month to be 30.44 days
		val days = (period.getYears * 12 + period.getMonths) * 30.44 + period.getDays
		(days * 24).hours
	}
}