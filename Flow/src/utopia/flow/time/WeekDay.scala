package utopia.flow.time

import utopia.flow.operator.SelfComparable

import java.time.{DayOfWeek, LocalDate}
import scala.language.implicitConversions

/**
  * An enumeration for day of week (Monday to Sunday)
  * @author Mikko Hilpinen
  * @since 30.7.2020, v1.8
  */
sealed trait WeekDay extends SelfComparable[WeekDay]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Index of this week day [1, 7] where Monday is 1
	  */
	val index: Int
	
	/**
	  * @return A java counterpart to this week day
	  */
	def toJava: DayOfWeek
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Next week day
	  */
	def next = this + 1
	
	/**
	  * @return Previous week day
	  */
	def previous = this - 1
	
	/**
	  * @return An infinite iterator that starts from this week day and moves to the next week day
	  */
	def iterate = WeekDay.iterate(this)
	
	/**
	  * @return An infinite iterator that starts from this week day and moves back to previous day
	  */
	def reverseIterate = WeekDay.reverseIterate(this)
	
	
	// IMPLEMENTED	--------------------
	
	override def compareTo(o: WeekDay) = index.compareTo(o.index)
	
	
	// OTHER	------------------------
	
	/**
	  * @param dayCount Amount of days to move forwards
	  * @return Weekday after 'dayCount' days from this day
	  */
	def +(dayCount: Int) = WeekDay.forIndex(index + dayCount)
	
	/**
	  * @param dayCount Amount of days to move backwards
	  * @return Weekday before 'dayCount' days from this day
	  */
	def -(dayCount: Int) = this + (-dayCount)
	
	/**
	  * @param anotherDay Another week day
	  * @return Period from the other week day to this day
	  */
	def -(anotherDay: WeekDay) =
	{
		val days = {
			if (this >= anotherDay)
				index - anotherDay.index
			else
				index + (7 - anotherDay.index)
		}
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
	
	/**
	  * All week days from monday to sunday
	  */
	val values = Vector[WeekDay](Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday)
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Current week day (in local calendar)
	  */
	def current(): WeekDay = LocalDate.now().getDayOfWeek
	
	
	// IMPLICIT	------------------------
	
	// Automatically converts java day of week to a week day
	implicit def autoConversion(d: DayOfWeek): WeekDay = values.find { _.toJava == d }.get
	
	
	// OTHER	------------------------
	
	/**
	  * @param dayIndex Week day index where monday is 1 and sunday is 7
	  * @return Weekday matching specified index
	  */
	def forIndex(dayIndex: Int) =
	{
		if (dayIndex > 0)
			values((dayIndex - 1) % 7)
		else
			values(7 + ((dayIndex - 1) % 7))
	}
	
	/**
	  * @param startDay First week day to return
	  * @return An infinite iterator that iterates over weekdays
	  */
	def iterate(startDay: WeekDay = Monday) = Iterator.iterate(startDay) { _.next }
	
	/**
	  * @param startDay First week day to return
	  * @return An infinite iterator that iterates over weekdays in reverse order
	  */
	def reverseIterate(startDay: WeekDay = Sunday) = Iterator.iterate(startDay) { _.previous }
	
	
	// NESTED	------------------------
	
	object Monday extends WeekDay
	{
		override val index = 1
		override def self = this
		override def toJava = DayOfWeek.MONDAY
		override def toString = "Monday"
	}
	
	object Tuesday extends WeekDay
	{
		override val index = 2
		override def self = this
		override def toJava = DayOfWeek.TUESDAY
		override def toString = "Tuesday"
	}
	
	object Wednesday extends WeekDay
	{
		override val index = 3
		override def self = this
		override def toJava = DayOfWeek.WEDNESDAY
		override def toString = "Wednesday"
	}
	
	object Thursday extends WeekDay
	{
		override val index = 4
		override def self = this
		override def toJava = DayOfWeek.THURSDAY
		override def toString = "Thursday"
	}
	
	object Friday extends WeekDay
	{
		override val index = 5
		override def self = this
		override def toJava = DayOfWeek.FRIDAY
		override def toString = "Friday"
	}
	
	object Saturday extends WeekDay
	{
		override val index = 6
		override def self = this
		override def toJava = DayOfWeek.SATURDAY
		override def toString = "Saturday"
	}
	
	object Sunday extends WeekDay
	{
		override val index = 7
		override def self = this
		override def toJava = DayOfWeek.SUNDAY
		override def toString = "Sunday"
	}
}
