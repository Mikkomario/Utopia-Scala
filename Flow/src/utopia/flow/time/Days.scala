package utopia.flow.time

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.DaysType
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.SelfComparable

import java.time.{LocalDate, Period}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

object Days
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * 0 Days
	  */
	val zero = apply(0)
	/**
	  * 1 Week (7 days)
	  */
	val week = apply(7)
	
	
	// IMPLICIT    ------------------------
	
	// Implicitly converts a number of days to a duration
	implicit def daysToDuration(d: Days): FiniteDuration = d.toDuration
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param date1 A date
	  * @param date2 Another date
	  * @return The amount of days between these two dates.
	  *         Please note that the result is always positive, regardless of parameter order.
	  */
	def between(date1: LocalDate, date2: LocalDate) =
		Days(java.time.Duration.between(date1, date2).toDays.toInt.abs)
}

/**
  * Represents a period of time in unit of days. Similar to Period, except that this class doesn't handle
  * unit of months or years because of their ambiguity
  * @author Mikko Hilpinen
  * @since 27.6.2021, v1.10
  */
case class Days(length: Int) extends SelfComparable[Days] with ValueConvertible
{
	// COMPUTED -----------------------------
	
	/**
	  * @return A period equal to this length of time
	  */
	def toPeriod = Period.ofDays(length)
	/**
	  * @return A duration of time equal to this instance
	  */
	def toDuration = Duration(length, TimeUnit.DAYS)
	
	/**
	  * @return The absolute value of this length of days
	  */
	def abs = Days(length.abs)
	
	/**
	  * @return A negative copy of this length of days
	  */
	def unary_- = Days(-length)
	
	
	// IMPLEMENTED ---------------------------
	
	override def self = this
	
	override def toString = if (length == 1) "a day" else s"$length days"
	
	override implicit def toValue: Value = new Value(Some(this), DaysType)
	
	override def compareTo(o: Days) = length.compareTo(o.length)
	
	
	// OTHER    ------------------------------
	
	def +(other: Days) = Days(length + other.length)
	def -(other: Days) = Days(length - other.length)
	
	def +(period: Period) = period.plusDays(length)
	def -(period: Period) = period.minusDays(length)
	
	def +(duration: Duration) = toDuration + duration
	def -(duration: Duration) = toDuration - duration
	
	def +(amount: Int) = Days(length + amount)
	def -(amount: Int) = Days(length - amount)
	
	def *(mod: Int) = Days(length * mod)
	def *(mod: Double) = toDuration * mod
	
	def /(mod: Double) = toDuration / mod
	
	def <(duration: Duration) = toDuration < duration
	def >(duration: Duration) = toDuration > duration
	def <=(duration: Duration) = !(this > duration)
	def >=(duration: Duration) = !(this < duration)
	
	/**
	  * @param date Origin date
	  * @return A day this amount of days before the specified date
	  */
	def before(date: LocalDate) = date.minusDays(length)
	/**
	  * @param date Origin date
	  * @return A day this amount of days after the specified date
	  */
	def after(date: LocalDate) = date.plusDays(length)
	
	/**
	  * @param date A date
	  * @return Whether this amount of days has passed since that date
	  */
	def hasPassedSince(date: LocalDate) = Today >= date.plusDays(length)
}
