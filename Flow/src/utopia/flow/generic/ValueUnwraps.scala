package utopia.flow.generic

import utopia.flow.collection.value.typeless.Value

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime}
import utopia.flow.time.Days

import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
 * Provides implicit methods for unwrapping values into basic data types
 * @author Mikko Hilpinen
 * @since 9.5.2020, v1.8
 */
object ValueUnwraps
{
	implicit def valueToStringOption(v: Value): Option[String] = v.string
	implicit def valueToString(v: Value): String = v.getString

	implicit def valueToIntOption(v: Value): Option[Int] = v.int
	implicit def valueToInt(v: Value): Int = v.getInt

	implicit def valueToDoubleOption(v: Value): Option[Double] = v.double
	implicit def valueToDouble(v: Value): Double = v.getDouble

	implicit def valueToLongOption(v: Value): Option[Long] = v.long
	implicit def valueToLong(v: Value): Long = v.getLong

	implicit def valueToBooleanOption(v: Value): Option[Boolean] = v.boolean
	implicit def valueToBoolean(v: Value): Boolean = v.getBoolean

	implicit def valueToInstantOption(v: Value): Option[Instant] = v.instant
	implicit def valueToInstant(v: Value): Instant = v.getInstant

	implicit def valueToLocalDateOption(v: Value): Option[LocalDate] = v.localDate
	implicit def valueToLocalDate(v: Value): LocalDate = v.getLocalDate

	implicit def valueToLocalTimeOption(v: Value): Option[LocalTime] = v.localTime
	implicit def valueToLocalTime(v: Value): LocalTime = v.getLocalTime

	implicit def valueToLocalDateTimeOption(v: Value): Option[LocalDateTime] = v.localDateTime
	implicit def valueToLocalDateTime(v: Value): LocalDateTime = v.getLocalDateTime

	implicit def valueToFloatOption(v: Value): Option[Float] = v.float
	implicit def valueToFloat(v: Value): Float = v.getFloat
	
	implicit def valueToDuration(v: Value): FiniteDuration = v.getDuration
	implicit def valueToDurationOption(v: Value): Option[FiniteDuration] = v.duration
	
	implicit def valueToDays(v: Value): Days = v.getDays
	implicit def valueToDaysOption(v: Value): Option[Days] = v.days
}
