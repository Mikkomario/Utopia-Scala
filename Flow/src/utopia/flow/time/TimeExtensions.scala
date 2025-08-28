package utopia.flow.time

import utopia.flow.operator.equality.ApproxEquals
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.time.TimeUnit.{Day, Hour, JTimeUnit, MicroSecond, MilliSecond, Minute, NanoSecond, Second, Week}

import java.time._
import java.time.format.DateTimeFormatter
import java.time.temporal.{ChronoUnit, TemporalAmount}
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions
import scala.util.Try

/**
  * This object contains some extensions for java's time classes
  * @author Mikko Hilpinen
  * @since 17.11.2018
  * */
object TimeExtensions
{
	// TYPES    ---------------------------
	
	/**
	  * Scala's Duration data type. May be useful in situations where java.time.Duration is imported.
	  */
	type SDuration = scala.concurrent.duration.Duration
	
	
	// EXTENSIONS   -----------------------
	
	implicit class ExtendedInstant(val i: Instant)
		extends AnyVal with SelfComparable[Instant] with ApproxEquals[Instant]
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
		
		override def compareTo(o: Instant) = i.compareTo(o)
		
		/**
		  * @param other Another instant
		  * @return Whether these two instants are the same, milliseconds-wise
		  */
		override def ~==(other: Instant) = i.toEpochMilli == other.toEpochMilli
		
		
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
		def +(amount: TemporalAmount) = {
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
		/**
		  * @param amount Amount of duration to move this instant
		  * @return An instant after specified duration has passed from this instant
		  */
		def +(amount: FiniteDuration) = i.plus(amount.length, amount.unit)
		/**
		  * @param amount Amount of duration to advance this instant (may be infinite)
		  * @return An instant after 'amount' from this instant. Maximum instant if the specified duration is infinite.
		  */
		def +(amount: SDuration): Instant = amount match {
			case d: FiniteDuration => this + d
			case scala.concurrent.duration.Duration.Inf => Instant.MAX
			case scala.concurrent.duration.Duration.MinusInf => Instant.MIN
			case _ => throw new IllegalArgumentException(s"Can't add $amount")
		}
		/**
		  * An instant before the specified duration
		  */
		def -(amount: TemporalAmount) = amount match {
			case period: Period =>
				// See + for explanation
				if (period.getMonths != 0 || period.getYears != 0)
					toLocalDateTime.minus(period).toInstantInDefaultZone
				else
					i.minus(period)
			
			case _ => i.minus(amount)
		}
		/**
		  * @param amount Amount of time to subtract
		  * @return The instant before this instant by specified duration
		  */
		def -(amount: FiniteDuration) = i.minus(amount.length, amount.unit)
		/**
		  * @param duration Amount of time to subtract (may be infinite)
		  * @return An instant before this one by the specified duration (minimum instant if the duration is infinite)
		  */
		def -(duration: SDuration): Instant = duration match {
			case d: FiniteDuration => this - d
			case scala.concurrent.duration.Duration.Inf => Instant.MIN
			case scala.concurrent.duration.Duration.MinusInf => Instant.MAX
			case _ => throw new IllegalArgumentException(s"Can't subtract $duration")
		}
		/**
		  * Finds the difference (duration) between the two time instances
		  */
		def -(time: Instant) = Duration.between(time, i)
		
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
		  * @param duration A duration of time
		  * @return Copy of this date time after the specified amount of duration has passed
		  */
		def +(duration: SDuration) = duration match {
			case duration: FiniteDuration => d.plus(duration.length, duration.unit)
			case scala.concurrent.duration.Duration.Inf => LocalDateTime.MAX
			case scala.concurrent.duration.Duration.MinusInf => LocalDateTime.MIN
			case _ => throw new IllegalArgumentException(s"Can't append $duration to a local date time instance")
		}
		/**
		  * @param duration A duration of time
		  * @return Copy of this date time before teh specified amount of duration had passed
		  */
		def -(duration: SDuration) = this + (-duration)
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
	
	implicit class ExtendedScalaDuration(val d: SDuration) extends AnyVal
	{
		// COMPUTED -------------------------------
		
		/**
		  * @return Whether this duration is an infinite duration.
		  *         Note: Also returns true in case of Duration.Undefined.
		  */
		def isInfinite = !d.isFinite
		
		/**
		  * @return A finite version of this duration. None for infinite durations.
		  */
		def finite = d match {
			case f: FiniteDuration => Some(f)
			case d => if (d.isFinite) Some(FiniteDuration(d.length, d.unit)) else None
		}
		/**
		 * @return A finite version of this duration. Zero for infinite / invalid durations.
		 */
		def finiteOrZero = finite.getOrElse(scala.concurrent.duration.Duration.Zero)
		
		/**
		  * @return Describes this duration in a suitable unit and precision
		  */
		def description: String = d match {
			case d: FiniteDuration =>
				if (d.length == 0)
					s"0s"
				else {
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
							if (hoursPart.abs > 504) {
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
				}
			case scala.concurrent.duration.Duration.Inf => "infinite"
			case scala.concurrent.duration.Duration.MinusInf => "negative infinity"
			case scala.concurrent.duration.Duration.Undefined => "undefined"
			case _ => d.toString
		}
		
		
		// OTHER    ----------------------------------
		
		/**
		  * @param threshold A time threshold
		  * @return Whether this duration has passed since that time threshold
		  */
		def hasPassedSince(threshold: Instant) = finite match {
			case Some(d) => Now >= threshold + d
			case None => d < duration.Duration.Zero
		}
		/**
		  * @param threshold A time threshold
		  * @return Whether this duration has passed since that time threshold
		  */
		def hasPassedSince(threshold: LocalDateTime) = finite match {
			case Some(d) => Now.toLocalDateTime >= threshold + d
			case None => d < duration.Duration.Zero
		}
	}
	
	implicit class ExtendedFiniteDuration(val d: FiniteDuration) extends AnyVal
	{
		/**
		  * @return This duration in milliseconds, but with double precision (converted from nanoseconds)
		  */
		def toPreciseMillis = to(MilliSecond)
		/**
		  * @return This duration in seconds, but with double precision (converted from nanoseconds)
		  */
		def toPreciseSeconds = to(Second)
		/**
		  * @return This duration in minutes, but with double precision (converted from nanoseconds)
		  */
		def toPreciseMinutes = to(Minute)
		/**
		  * @return This duration in hours, but with double precision (converted from nanoseconds)
		  */
		def toPreciseHours = to(Hour)
		/**
		  * @return This duration in days, but with double precision (converted from nanoseconds)
		  */
		def toPreciseDays = to(Day)
		/**
		  * @return This duration in weeks, but with double precision (converted from nanoseconds)
		  */
		def toPreciseWeeks = to(Week)
		
		/**
		  * @param unit Targeted time unit
		  * @return The number of specified units in this duration
		  */
		def to(unit: TimeUnit) = unit.countIn(d)
		
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
	
	implicit class ExtendedLocalDate(val d: LocalDate)
		extends AnyVal with SelfComparable[LocalDate]
	{
		import utopia.flow.time.{Month, Year}
		
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
		def toDuration = {
			if (t.getNano == 0)
				t.toSecondOfDay.seconds
			else
				t.toNanoOfDay.nanos
		}
		
		
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
	
	/**
	  * Provides numeric functions for instances of [[scala.concurrent.duration.Duration]].
	  * Note The * (i.e. times) implementation doesn't correctly yield unit to the power of two,
	  * as that is not supported by the current data type structures.
	  * @param unit Unit in which comparisons and other functions are performed.
	  *             Affects, toX & fromX -functions, for example.
	  */
	case class DurationIsNumericIn(unit: TimeUnit) extends Numeric[scala.concurrent.duration.Duration]
	{
		override def fromInt(x: Int): duration.Duration = unit(x)
		override def parseString(str: String): Option[duration.Duration] = str.toDoubleOption.map { unit(_) }
		
		override def toInt(x: duration.Duration): Int = unit.countIn(x).toInt
		override def toLong(x: duration.Duration): Long = unit.countIn(x).toLong
		override def toFloat(x: duration.Duration): Float = unit.countIn(x).toFloat
		override def toDouble(x: duration.Duration): Double = unit.countIn(x)
		
		override def negate(x: duration.Duration): duration.Duration = x.neg()
		
		override def plus(x: duration.Duration, y: duration.Duration): duration.Duration = x.plus(y)
		override def minus(x: duration.Duration, y: duration.Duration): duration.Duration = x.minus(y)
		
		override def times(x: duration.Duration, y: duration.Duration): duration.Duration =
			unit(unit.countIn(x) * unit.countIn(y))
		
		override def compare(x: duration.Duration, y: duration.Duration): Int = x.compareTo(y)
	}
	
	trait TimeNumber extends Any
	{
		// ABSTRACT ------------------------------
		
		/**
		  * @param unit Targeted unit of time
		  * @return This number of specified units of time
		  */
		def *(unit: TimeUnit): FiniteDuration
		
		
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
		import utopia.flow.time.{Month, Year, YearMonth}
		
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
		
		override def *(unit: TimeUnit): FiniteDuration = unit(i)
		
		
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
		
		override def *(unit: TimeUnit): FiniteDuration = unit(d)
	}
	
	implicit class LongAsTimeNumber(val l: Long) extends AnyVal with TimeNumber
	{
		override def *(unit: TimeUnit): FiniteDuration = unit(l)
	}
	
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
	
	/**
	  * Converts a java duration to a scala duration
	  */
	implicit def javaDurationToScalaDuration(duration: java.time.Duration): FiniteDuration = {
		val secondsPart = Second(duration.getSeconds)
		if (duration.getNano == 0)
			secondsPart
		else
			secondsPart + NanoSecond(duration.getNano)
	}
	implicit def javaDurationToExtendedScalaDuration(duration: java.time.Duration): ExtendedScalaDuration =
		javaDurationToScalaDuration(duration)
	implicit def javaDurationToExtendedFiniteDuration(duration: java.time.Duration): ExtendedFiniteDuration =
		javaDurationToScalaDuration(duration)
	/**
	  * Converts a finite scala duration to a java duration
	  */
	implicit def scalaDurationToJavaDuration(duration: FiniteDuration): Duration =
		java.time.Duration.of(duration.length, duration.unit)
}
