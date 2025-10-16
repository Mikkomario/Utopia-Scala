package utopia.flow.time

import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.equality.ApproxEquals
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Duration.SDuration
import utopia.flow.time.TimeUnit.{Hour, JTimeUnit, MicroSecond, MilliSecond, Minute, NanoSecond, Second}
import utopia.flow.time.{Duration, Month, Year, YearMonth}

import java.time._
import java.time.chrono.ChronoPeriod
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalAmount}
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * This object contains some extensions for java's time classes
  * @author Mikko Hilpinen
  * @since 17.11.2018
  * */
object TimeExtensions
{
	// IMPLICIT ---------------------------
	
	/**
	 * Implicitly converts a Java time unit to a [[ChronoUnit]]
	 * @param unit Unit to convert
	 * @return Converted time unit
	 */
	implicit def timeUnitToChronoUnit(unit: JTimeUnit): ChronoUnit = unit match {
		case java.util.concurrent.TimeUnit.NANOSECONDS => ChronoUnit.NANOS
		case java.util.concurrent.TimeUnit.MICROSECONDS => ChronoUnit.MICROS
		case java.util.concurrent.TimeUnit.MILLISECONDS => ChronoUnit.MILLIS
		case java.util.concurrent.TimeUnit.SECONDS => ChronoUnit.SECONDS
		case java.util.concurrent.TimeUnit.MINUTES => ChronoUnit.MINUTES
		case java.util.concurrent.TimeUnit.HOURS => ChronoUnit.HOURS
		case java.util.concurrent.TimeUnit.DAYS => ChronoUnit.DAYS
	}
	
	// implicit def durationToJava(duration: Duration): JDuration = duration.toJava
	// implicit def durationToScala(duration: Duration): SDuration = duration.toScala
	
	
	// EXTENSIONS   -----------------------
	
	/**
	 * Provides + and - functions related to durations
	 * @tparam Repr Type of this class
	 */
	trait CanAppendJavaDuration[+Repr] extends Any with Combinable[Duration, Repr]
	{
		// ABSTRACT -----------------------
		
		/**
		 * @return Maximum value of this type
		 */
		protected def _max: Repr
		/**
		 * @return Minimum value of this type
		 */
		protected def _min: Repr
		
		/**
		 * @param amount Amount of time to add to this instant
		 * @return A copy of this instant after the specified duration
		 */
		def +(amount: TemporalAmount): Repr
		/**
		 * @param amount Amount of time to subtract from this instant
		 * @return A copy of this instant before the specified duration
		 */
		def -(amount: TemporalAmount): Repr
		
		/**
		 * @param amount Amount to add to this instant
		 * @param unit Unit in which 'amount' is given
		 * @return A copy of this instant after the specified duration
		 */
		protected def _plus(amount: Long, unit: JTimeUnit): Repr
		
		
		// OTHER    -----------------------
		
		/**
		 * @param amount Amount of duration to move this instant
		 * @return An instant after specified duration has passed from this instant
		 */
		def +(amount: FiniteDuration) = _plus(amount.length, amount.unit)
		/**
		 * @param amount Amount of duration to advance this instant (may be infinite)
		 * @return An instant after 'amount' from this instant. Maximum instant if the specified duration is infinite.
		 */
		def +(amount: SDuration): Repr = amount match {
			case d: FiniteDuration => this + d
			case scala.concurrent.duration.Duration.Inf => _max
			case scala.concurrent.duration.Duration.MinusInf => _min
			case _ => throw new IllegalArgumentException(s"Can't add $amount")
		}
		/**
		 * @param amount Amount of duration to advance this instant (may be infinite)
		 * @return An instant after 'amount' from this instant. Maximum instant if the specified duration is infinite.
		 */
		def +(amount: Duration): Repr = amount.tryLengthOrJava match {
			case Success(Right((amount, unit))) =>
				val (multiplier, jUnit) = unit.toJava
				_plus(amount * multiplier, jUnit)
				
			case Success(Left(jDuration)) => this + jDuration
			case Failure(error) =>
				amount.sign match {
					case Positive => _max
					case Negative => _min
					case _ => throw error
				}
		}
		/**
		 * @param amount Amount of time to subtract
		 * @return The instant before this instant by specified duration
		 */
		def -(amount: FiniteDuration) = this + (-amount)
		/**
		 * @param duration Amount of time to subtract (may be infinite)
		 * @return An instant before this one by the specified duration (minimum instant if the duration is infinite)
		 */
		def -(duration: SDuration): Repr = this + (-duration)
		/**
		 * @param duration Amount of time to subtract (may be infinite)
		 * @return An instant before this one by the specified duration (minimum instant if the duration is infinite)
		 */
		def -(duration: Duration): Repr = this + (-duration)
	}
	
	implicit class ExtendedInstant(val i: Instant)
		extends AnyVal with SelfComparable[Instant] with ApproxEquals[Instant] with CanAppendJavaDuration[Instant]
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
		def toUtcDateTime = i.atZone(ZoneOffset.UTC).toLocalDateTime
		@deprecated("Renamed to .toUtcDateTime", "v2.6")
		def toUTCDateTime = toUtcDateTime
		
		/**
		 * @return Whether this instant is in the past
		 */
		def isPast = this < Now
		/**
		 * @return Whether this instant is currently in the future
		 */
		def isFuture = this > Now
		@deprecated("Renamed to .isPast", "v2.2")
		def isInPast = isPast
		@deprecated("Renamed to .isFuture", "v2.2")
		def isInFuture = isFuture
		
		
		// IMPLEMENTED  ----------------------
		
		override def self = i
		
		override protected def _max: Instant = Instant.MAX
		override protected def _min: Instant = Instant.MIN
		
		override def compareTo(o: Instant) = i.compareTo(o)
		/**
		  * @param other Another instant
		  * @return Whether these two instants are the same, milliseconds-wise
		  */
		override def ~==(other: Instant) = i.toEpochMilli == other.toEpochMilli
		
		override def +(amount: TemporalAmount) = {
			amount match {
				case period: Period =>
					// Long time periods are a bit tricky because the actual length of traversed time depends on
					// the context (date + time zone + calendars etc.)
					// However, when the user asks Instant.now() + 3 months, we can estimate that he probably means
					// 3 months in the local time context
					if (period.getMonths != 0 || period.getYears != 0)
						toLocalDateTime.plus(period).toInstantInDefaultZone
					else
						i.plus(period)
				
				case _ => i.plus(amount)
			}
		}
		override def -(amount: TemporalAmount) = amount match {
			case period: Period =>
				// See + for explanation
				if (period.getMonths != 0 || period.getYears != 0)
					toLocalDateTime.minus(period).toInstantInDefaultZone
				else
					i.minus(period)
			
			case _ => i.minus(amount)
		}
		
		override protected def _plus(amount: Long, unit: JTimeUnit): Instant = i.plus(amount, unit)
		
		
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
		  * Finds the difference (duration) between the two time instances
		  */
		def -(time: Instant): Duration = java.time.Duration.between(time, i)
		/**
		  * @param other Another instant
		  * @return Time period from this instant to the other
		  */
		def until(other: Instant): Duration = other - i
	}
	
	implicit class ExtendedLocalDateTime(val d: LocalDateTime)
		extends AnyVal with SelfComparable[LocalDateTime] with CanAppendJavaDuration[LocalDateTime]
	{
		// COMPUTED	------------------------------
		
		/**
		  * @return Converts this date time to an instant. Expects this date time to be in system default zone
		  */
		def toInstantInDefaultZone = d.toInstant(ZoneId.systemDefault().getRules.getOffset(d))
		
		
		// IMPLEMENTED  --------------------------
		
		override def self = d
		
		override def compareTo(o: LocalDateTime) = d.compareTo(o)
		
		override protected def _max: LocalDateTime = LocalDateTime.MAX
		override protected def _min: LocalDateTime = LocalDateTime.MIN
		
		override protected def _plus(amount: Long, unit: JTimeUnit): LocalDateTime = d.plus(amount, unit)
		
		
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
		def -(other: LocalDateTime): Duration = java.time.Duration.between(other, d)
		/**
		  * @param other Another local date time
		  * @return The duration from this time point to the second time point
		  */
		def until(other: LocalDateTime): Duration = other - d
	}
	
	implicit class ExtendedScalaDuration(val d: SDuration) extends AnyVal
	{
		// COMPUTED -------------------------------
		
		/**
		  * @return A finite version of this duration. None for infinite durations.
		  */
		@deprecated("Deprecated for removal. Please use .ifFinite instead", "v2.7")
		def finite = d match {
			case f: FiniteDuration => Some(f)
			case d => if (d.isFinite) Some(FiniteDuration(d.length, d.unit)) else None
		}
	}
	
	implicit class ExtendedLocalDate(val d: LocalDate)
		extends AnyVal with SelfComparable[LocalDate] with CanAppendJavaDuration[LocalDateTime]
	{
		// COMPUTED	-------------------------
		
		/**
		  * @return Year of this date
		  */
		def year = Year(d.getYear)
		/**
		  * @return Month of this date
		  */
		def month = Month(d.getMonthValue)
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
		def yearMonth: utopia.flow.time.YearMonth = year(month)
		
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
		
		/**
		 * @return Whether this date is still in the future
		 */
		def isFuture = d.isAfter(Today)
		/**
		 * @return Whether this date has passed already
		 */
		def isPast = d.isBefore(Today)
		/**
		 * @return Whether this date is today
		 */
		def isToday = d.isEqualTo(Today)
		
		
		// IMPLEMENTED  --------------------
		
		override def self = d
		
		override protected def _max: LocalDateTime = LocalDateTime.MAX
		override protected def _min: LocalDateTime = LocalDateTime.MIN
		
		override def compareTo(o: LocalDate) = d.compareTo(o)
		
		override def +(amount: TemporalAmount): LocalDateTime = d.atStartOfDay() + amount
		override def -(amount: TemporalAmount): LocalDateTime = d.atStartOfDay() - amount
		
		override protected def _plus(amount: Long, unit: JTimeUnit): LocalDateTime = d.atStartOfDay().plus(amount, unit)
		
		
		// OTHER	------------------------
		
		/**
		  * Adds a number of days to this date
		  * @param timePeriod A time period to add
		  * @return A modified copy of this date
		  */
		def +(timePeriod: Period) = d.plus(timePeriod)
		def +(days: Days) = d.plusDays(days.length)
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
		  * Subtracts a number of days to this date. E.g. 2.1.2001 + 3 hours would be 2.1.2001 03:00.
		  * @param timePeriod A time period to subtract
		  * @return A modified copy of this date
		  */
		def -(timePeriod: Period) = d.minus(timePeriod)
		def -(days: Days) = d.minusDays(days.length)
		/**
		  * @param other Another date
		  * @return Time period between these two dates (from specified date to this date)
		  */
		def -(other: LocalDate) = Days((d.toEpochDay - other.toEpochDay).toInt)
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
	
	implicit class ExtendedLocalTime(val t: LocalTime)
		extends AnyVal with SelfComparable[LocalTime] with CanAppendJavaDuration[LocalTime]
	{
		// COMPUTED	----------------------
		
		/**
		  * @return A duration based on this time element (from the beginning of day)
		  */
		def toDuration = {
			if (t.getMinute == 0)
				t.getHour.hours
			else if (t.getSecond == 0)
				Minute(t.getHour * 60 + t.getMinute)
			else if (t.getNano == 0)
				t.toSecondOfDay.seconds
			else
				t.toNanoOfDay.nanos
		}
		
		
		// IMPLEMENTED  ------------------
		
		override def self = t
		
		override protected def _max: LocalTime = LocalTime.MAX
		override protected def _min: LocalTime = LocalTime.MIN
		
		override def compareTo(o: LocalTime) = t.compareTo(o)
		
		override def +(amount: TemporalAmount): LocalTime = amount match {
			case _: Period => t
			case _: ChronoPeriod => t
			case amount => t.plus(amount)
		}
		override def -(amount: TemporalAmount): LocalTime = amount match {
			case _: Period => t
			case _: ChronoPeriod => t
			case amount => t.minus(amount)
		}
		
		override protected def _plus(amount: Long, unit: JTimeUnit): LocalTime =
			if (unit == java.util.concurrent.TimeUnit.DAYS) t else t.plus(amount, unit)
		
		
		// OTHER	----------------------
		
		/**
		 * @param other Another time
		 * @return Duration from 'other' to this. Negative if 'other' comes after this.
		 */
		def -(other: LocalTime) = toDuration - other.toDuration
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
	
	trait TimeNumber extends Any
	{
		// ABSTRACT ------------------------------
		
		/**
		  * @param unit Targeted unit of time
		  * @return This number of specified units of time
		  */
		def *(unit: TimeUnit): Duration
		
		
		// COMPUTED ------------------------------
		
		/**
		  * @return This number of nanoseconds
		  */
		def nanos = this * NanoSecond
		/**
		  * @return This number of microseconds
		  */
		def microSeconds = this * MicroSecond
		/**
		  * @return This number of milliseconds
		  */
		def millis = this * MilliSecond
		/**
		  * @return This number of seconds
		  */
		def seconds = this * Second
		/**
		  * @return This number of minutes
		  */
		def minutes = this * Minute
		/**
		  * @return This number of hours
		  */
		def hours = this * Hour
	}
	
	implicit class IntAsTimeNumber(val i: Int) extends AnyVal with TimeNumber
	{
		// COMPUTED ------------------------
		
		/**
		  * @return This number as a local hours number
		  */
		def oClock = LocalTime.of(i, 0)
		
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
		def month = Month(i)
		/**
		  * @return This number as a year
		  */
		def year = Year(i)
		
		
		// IMPLEMENTED  --------------------
		
		override def *(unit: TimeUnit): Duration = unit(i)
		
		
		// OTHER    ------------------------
		
		/**
		  * Converts two integers into a local time
		  * @param minutes Minutes to add to this hour
		  * @return "This" hour at 'minutes' minutes.
		  *         E.g. 17::54 is interpreted as 17:54
		  */
		def ::(minutes: Int) = LocalTime.of(i, minutes)
		
		/**
		  * Combines this minutes value with hours in order to form a local time value
		  * @param hour Target hour
		  * @return A local time value that's "this" many minutes past / after 'hour' hour.
		  *         E.g. 6 past 12 would yield 12:06
		  */
		def past(hour: Int) = if (hour == 24) LocalTime.of(0, i) else LocalTime.of(hour, i)
		
		/**
		  * @param month Target month
		  * @return This day at that month
		  */
		def of(month: Month) = month(i)
		/**
		  * @param month Target month
		  * @return This day at that month
		  */
		def dayOf(month: Int) = Month(month)(i)
		
		/**
		  * @param year Targeted year
		  * @return This month at that year
		  */
		def of(year: Year) = year(Month(i))
		/**
		  * @param year Targeted year
		  * @return This month at that year
		  */
		def monthOf(year: Int) = YearMonth(Year(year), Month(i))
		
		/**
		  * @param month Targeted month
		  * @return This day at that month
		  */
		def /(month: Month) = month(i)
		/**
		  * @param year Targeted year
		  * @return This month at that year
		  */
		def /(year: Year) = year(Month(i))
	}
	
	implicit class DoubleAsTimeNumber(val d: Double) extends AnyVal with TimeNumber
	{
		// IMPLEMENTED  --------------------
		
		override def *(unit: TimeUnit): Duration = unit(d)
	}
	
	implicit class LongAsTimeNumber(val l: Long) extends AnyVal with TimeNumber
	{
		override def *(unit: TimeUnit): Duration = unit(l)
	}
}
