package utopia.flow.time

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.ordering.UsesSelfOrdering
import utopia.flow.util.StringExtensions._

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
	
	/**
	 * @return Characters, when appearing in a string in order, may represent this weekday
	 */
	protected def keyChars: String
	
	
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
	 * Finds a week day that best matches the specified string.
	 * Supports multiple string formats.
	 * E.g. Monday matches to all of (not exclusively): "mo", "md", "mon", "monday"
	 * @param dayName A string that represents a week day name
	 * @return Week day that best matched the specified name.
	 *         None if none of the days matched, or if input was too ambiguous (e.g. for "day" or "u").
	 */
	def matching(dayName: String) = {
		// Reduces the number of valid options by making sure all input characters appear in the weekday names
		_values.filter { _.toString.containsCharsInOrder(dayName, ignoreCase = true) }.emptyOneOrMany.flatMap {
			// Case: Only one valid option remains => Returns that one
			case Left(only) => Some(only)
			// Case: Multiple valid options remain
			case Right(options) =>
				// Prioritizes days that start with the specified string (E.g. Monday with "mo")
				options.find { _.toString.startsWithIgnoreCase(dayName) }
					.orElse {
						// If no match was found, attempts to find an option using "key characters"
						// E.g. Tuesday using "ts"
						options.find { d => dayName.containsCharsInOrder(d.keyChars, ignoreCase = true) }
					}
		}
	}
	
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
		override val toString = "Monday"
		override protected val keyChars: String = "md"
		override def toJava = DayOfWeek.MONDAY
	}
	object Tuesday extends WeekDay
	{
		override val toString = "Tuesday"
		override protected val keyChars: String = "ts"
		override def toJava = DayOfWeek.TUESDAY
		
	}
	object Wednesday extends WeekDay
	{
		override val toString = "Wednesday"
		override protected val keyChars: String = "wd"
		override def toJava = DayOfWeek.WEDNESDAY
	}
	
	object Thursday extends WeekDay
	{
		override val toString = "Thursday"
		override protected val keyChars: String = "th"
		override def toJava = DayOfWeek.THURSDAY
	}
	object Friday extends WeekDay
	{
		override val toString = "Friday"
		override protected val keyChars: String = "fr"
		override def toJava = DayOfWeek.FRIDAY
	}
	object Saturday extends WeekDay
	{
		override val toString = "Saturday"
		override protected val keyChars: String = "st"
		override def toJava = DayOfWeek.SATURDAY
	}
	
	object Sunday extends WeekDay
	{
		override val toString = "Sunday"
		override protected val keyChars: String = "sn"
		override def toJava = DayOfWeek.SUNDAY
	}
}
