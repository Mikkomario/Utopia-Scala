package utopia.flow.time

import utopia.flow.operator.UsesSelfOrdering

import java.time.{DayOfWeek, LocalDate}
import scala.language.implicitConversions

/**
  * An enumeration for day of week (Monday to Sunday)
  * @author Mikko Hilpinen
  * @since 30.7.2020, v1.8
  */
sealed trait WeekDay extends UsesSelfOrdering[WeekDay]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return A java counterpart to this week day
	  */
	def toJava: DayOfWeek
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return The week day previous to this one
	  */
	def previous: WeekDay = WeekDay(_index - 1)
	/**
	  * @return The next week day compared to this one
	  */
	def next: WeekDay = WeekDay(_index + 1)
	
	/**
	  * @return An infinite iterator that starts from this week day and moves to the next week day
	  */
	def iterate = Iterator.iterate(this) { _.next }
	/**
	  * @return An infinite iterator that starts from this week day and moves back to previous day
	  */
	def reverseIterate = Iterator.iterate(this) { _.previous }
	
	/**
	  * @param w Week calendar system
	  * @return The index of this weekday [0,6] in that system
	  */
	def index(implicit w: WeekDays) = w.indexOf(this)
	
	// Index used in private operations that are the same regardless of week start day
	private def _index = WeekDay._values.indexOf(this)
	
	
	// IMPLEMENTED  --------------------
	
	override def self: WeekDay = this
	
	
	// OTHER	------------------------
	
	/**
	  * @param dayCount Amount of days to move forwards
	  * @return Weekday after 'dayCount' days from this day
	  */
	def +(dayCount: Int) = WeekDay(_index + dayCount)
	/**
	  * @param days Amount of days to move forwards
	  * @return Weekday after 'dayCount' days from this day
	  */
	def +(days: Days): WeekDay = this + days.toDays.toInt
	/**
	  * @param dayCount Amount of days to move backwards
	  * @return Weekday before 'dayCount' days from this day
	  */
	def -(dayCount: Int) = this + (-dayCount)
	/**
	  * @param days Amount of days to move backwards
	  * @return Weekday before 'dayCount' days from this day
	  */
	def -(days: Days): WeekDay = this - days.toDays.toInt
	
	/**
	  * @param anotherDay Another week day
	  * @return Period from the other week day to this day
	  */
	def -(anotherDay: WeekDay) = {
		val myIndex = _index
		val theirIndex = anotherDay._index
		val days = if (myIndex >= theirIndex) myIndex - theirIndex else myIndex + (7 - theirIndex)
		Days(days)
	}
	/**
	  * @param anotherDay Another week day
	  * @return Period from this week day to that other week day
	  */
	def until(anotherDay: WeekDay) = anotherDay - this
}

object WeekDay
{
	// ATTRIBUTES	--------------------
	
	private val _values = Vector[WeekDay](Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
	
	
	// COMPUTED	------------------------
	
	/**
	  * All week days from monday to sunday
	  */
	@deprecated("Please use WeekDays instead", "v2.1")
	def values = _values
	
	/**
	  * @return Current week day (in local calendar)
	  */
	def current: WeekDay = LocalDate.now().getDayOfWeek
	
	
	// IMPLICIT	------------------------
	
	// Automatically converts java day of week to a week day
	implicit def autoConversion(d: DayOfWeek): WeekDay = _values.find { _.toJava == d }.get
	
	
	// OTHER	------------------------
	
	/**
	  * @param dayIndex Week day index where monday is 1 and sunday is 7
	  * @return Weekday matching specified index
	  */
	@deprecated("Please use WeekDays instead", "v2.1")
	def forIndex(dayIndex: Int) = {
		if (dayIndex > 0)
			values((dayIndex - 1) % 7)
		else
			values(7 + ((dayIndex - 1) % 7))
	}
	
	/**
	  * @param startDay First week day to return
	  * @param w        Week calendar system
	  * @return An infinite iterator that iterates over weekdays
	  */
	@deprecated("Please use startDay.iterate instead", "v2.1")
	def iterate(startDay: WeekDay = Monday)(implicit w: WeekDays) = startDay.iterate
	/**
	  * @param startDay First week day to return
	  * @param w        Week calendar system
	  * @return An infinite iterator that iterates over weekdays in reverse order
	  */
	@deprecated("Please use startDay.reverseIterate instead", "v2.1")
	def reverseIterate(startDay: WeekDay = Sunday)(implicit w: WeekDays) = startDay.reverseIterate
	
	// Assumes Monday to Sunday - Not suitable for public exposure
	private def apply(index: Int) = if (index >= 0) _values(index % 7) else _values(7 + (index % 7))
	
	
	// NESTED	------------------------
	
	object Monday extends WeekDay
	{
		override def toJava = DayOfWeek.MONDAY
		override def toString = "Monday"
	}
	
	object Tuesday extends WeekDay
	{
		override def toJava = DayOfWeek.TUESDAY
		override def toString = "Tuesday"
	}
	
	object Wednesday extends WeekDay
	{
		override def toJava = DayOfWeek.WEDNESDAY
		override def toString = "Wednesday"
	}
	
	object Thursday extends WeekDay
	{
		override def toJava = DayOfWeek.THURSDAY
		override def toString = "Thursday"
	}
	
	object Friday extends WeekDay
	{
		override def toJava = DayOfWeek.FRIDAY
		override def toString = "Friday"
	}
	
	object Saturday extends WeekDay
	{
		override def toJava = DayOfWeek.SATURDAY
		override def toString = "Saturday"
	}
	
	object Sunday extends WeekDay
	{
		override def toJava = DayOfWeek.SUNDAY
		override def toString = "Sunday"
	}
}
