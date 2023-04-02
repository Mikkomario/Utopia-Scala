package utopia.flow.time

import java.time.{DayOfWeek, LocalDate}
import scala.language.implicitConversions
import scala.math.Ordered.orderingToOrdered

/**
  * An enumeration for day of week (Monday to Sunday)
  * @author Mikko Hilpinen
  * @since 30.7.2020, v1.8
  */
sealed trait WeekDay
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return A java counterpart to this week day
	  */
	def toJava: DayOfWeek
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return An infinite iterator that starts from this week day and moves to the next week day
	  */
	def iterate(implicit w: WeekDays) = WeekDay.iterate(this)
	/**
	  * @return An infinite iterator that starts from this week day and moves back to previous day
	  */
	def reverseIterate(implicit w: WeekDays) = WeekDay.reverseIterate(this)
	
	/**
	  * @param w Week calendar system
	  * @return The index of this weekday [0,6] in that system
	  */
	def index(implicit w: WeekDays) = w.indexOf(this)
	
	/**
	  * @param w Week calendar system
	  * @return Next week day
	  */
	def next(implicit w: WeekDays) = this + 1
	/**
	  * @param w Week calendar system
	  * @return Previous week day
	  */
	def previous(implicit w: WeekDays) = this - 1
	
	
	// OTHER	------------------------
	
	/**
	  * @param dayCount Amount of days to move forwards
	  * @param w Week calendar system
	  * @return Weekday after 'dayCount' days from this day
	  */
	def +(dayCount: Int)(implicit w: WeekDays) = w(index + dayCount)
	/**
	  * @param days Amount of days to move forwards
	  * @param w        Week calendar system
	  * @return Weekday after 'dayCount' days from this day
	  */
	def +(days: Days)(implicit w: WeekDays): WeekDay = this + days.toDays.toInt
	/**
	  * @param dayCount Amount of days to move backwards
	  * @param w Week calendar system
	  * @return Weekday before 'dayCount' days from this day
	  */
	def -(dayCount: Int)(implicit w: WeekDays) = this + (-dayCount)
	/**
	  * @param days Amount of days to move backwards
	  * @param w        Week calendar system
	  * @return Weekday before 'dayCount' days from this day
	  */
	def -(days: Days)(implicit w: WeekDays): WeekDay = this - days.toDays.toInt
	
	/**
	  * @param anotherDay Another week day
	  * @param w        Week calendar system
	  * @return Period from the other week day to this day
	  */
	def -(anotherDay: WeekDay)(implicit w: WeekDays) = {
		val days = if (this >= anotherDay) index - anotherDay.index else index + (7 - anotherDay.index)
		Days(days)
	}
	/**
	  * @param anotherDay Another week day
	  * @param w        Week calendar system
	  * @return Period from this week day to that other week day
	  */
	def until(anotherDay: WeekDay)(implicit w: WeekDays) = anotherDay - this
}

// TODO: Add support for weeks that start on sunday
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
	def iterate(startDay: WeekDay = Monday)(implicit w: WeekDays) =
		Iterator.iterate(startDay) { _.next }
	/**
	  * @param startDay First week day to return
	  * @param w        Week calendar system
	  * @return An infinite iterator that iterates over weekdays in reverse order
	  */
	def reverseIterate(startDay: WeekDay = Sunday)(implicit w: WeekDays) =
		Iterator.iterate(startDay) { _.previous }
	
	
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
