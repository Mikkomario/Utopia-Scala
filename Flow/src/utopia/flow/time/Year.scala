package utopia.flow.time

import utopia.flow.operator.combine.{Combinable, Subtractable}
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.{Reversible, Steppable}

import java.time
import java.time.LocalDate
import scala.language.implicitConversions

object Year
{
	// TYPES    ------------------------------
	
	/**
	 * A Java version of this class
	 */
	type JYear = time.Year
	
	
	// IMPLICIT ------------------------------
	
	// Implicitly converts from integer
	implicit def intToYear(year: Int): Year = apply(year)
	// Implicitly converts from & to a Java Year
	implicit def apply(year: JYear): Year = apply(year.getValue)
	
	implicit def toJava(year: Year): JYear = year.toJava
}

/**
 * Represents a year (in the Gregorian calendar system)
 *
 * @author Mikko Hilpinen
 * @since 04.06.2025, v2.7
 */
case class Year(value: Int)
	extends SelfComparable[Year] with Combinable[Int, Year] with Subtractable[Int, Year]
		with Reversible[Year] with Steppable[Year]
{
	// COMPUTED ------------------------------
	
	/**
	 * @return The year after this one
	 */
	def next = more
	/**
	 * @return The year previous to this one
	 */
	def previous = less
	
	/**
	 * @return The length of this year
	 */
	def length = Days(if (isLeap) 366 else 365)
	/**
	 * @return Whether this is a leap year
	 */
	def isLeap = java.time.Year.isLeap(value)
	
	/**
	  * @return The first day of this year (i.e. 1st of January)
	  */
	def firstDay = LocalDate.of(value, 1, 1)
	/**
	  * @return The last day of this year (i.e. 31st of December)
	  */
	def lastDay = LocalDate.of(value, 12, 31)
	/**
	  * @return Dates of this year
	  */
	def dates = DateRange.inclusive(firstDay, lastDay)
	
	/**
	 * @return A Java model of this year
	 */
	def toJava = time.Year.of(value)
	
	
	// IMPLEMENTED  --------------------------
	
	override def self = this
	
	override def unary_- : Year = Year(-value)
	
	override def toString = value.toString
	
	override def +(other: Int): Year = Year(value + other)
	override def -(years: Int) = Year(value - years)
	override def compareTo(o: Year) = value.compareTo(o.value)
	
	override def next(direction: Sign): Year = Year(value + direction.modifier)
	override def is(extreme: Extreme): Boolean = false
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param month Targeted month
	  * @return That month at this year
	  */
	def apply(month: Month) = YearMonth(this, month)
	/**
	  * @param monthOfYear Value of the targeted month [1,12]
	  * @return That month at this year
	  */
	@throws[IndexOutOfBoundsException]("If 'monthOfYear' is out of range")
	def apply(monthOfYear: Int): YearMonth = apply(Month(monthOfYear))
	/**
	  * @param monthDay Targeted month & day of month
	  * @return That day at this year
	  */
	def apply(monthDay: MonthDay) = LocalDate.of(value, monthDay.month.value, monthDay.day)
	/**
	  * @param range A range of dates
	  * @return The portion of that range that overlaps with this year. 0-2 different ranges.
	  */
	def apply(range: YearlyDateRange): IndexedSeq[DateRange] = range.at(this)
	
	/**
	  * Adds month information to this year
	  * @param month Targeted month
	  * @return A monthYear based on this year and specified month
	  */
	def +(month: Month) = apply(month)
	/**
	  * Adds month information (same as this + month)
	  * @param month targeted month
	  * @return targeted month on this year
	  */
	def /(month: Month) = apply(month)
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
}
