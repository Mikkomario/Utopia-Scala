package utopia.flow.time

import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Month.February

import scala.language.implicitConversions

/**
 * An enumeration for different months (in the Gregorian calendar system)
 *
 * @author Mikko Hilpinen
 * @since 04.06.2025, v2.7
 */
sealed trait Month extends MonthLike[Month]
{
	// ABSTRACT ----------------------
	
	/**
	 * @return A 1-based index matching this month
	 */
	def value: Int
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return Whether this month has a different length on leap years
	 */
	def isDifferentOnLeapYear = this == February
	
	/**
	 * @return A Java model of this month
	 */
	def toJava = java.time.Month.of(value)
	
	
	// IMPLEMENTED  -------------------
	
	override def self = this
	
	override def toString = name
	
	override def +(other: Int) = {
		val newIndex = (value - 1 + other) % 12
		if (newIndex < 0)
			Month.values(12 - newIndex)
		else
			Month.values(newIndex)
	}
	override def -(other: Int): Month = this + (-other)
	
	override def compareTo(o: Month) = value.compareTo(o.value)
	
	
	// OTHER    ----------------------
	
	/**
	 * @param dayOfMonth Day of this month
	 * @param assumeLeapYear Whether to assume a leap year (default = false)
	 * @return Specified day of this month
	 */
	def apply(dayOfMonth: Int, assumeLeapYear: Boolean = false) = MonthDay(this, dayOfMonth, assumeLeapYear)
	
	/**
	 * @param year A year
	 * @return Length of this month that year
	 */
	def lengthAt(year: Year): Days = lengthAt(leapYear = year.isLeap)
	/**
	 * @param leapYear Whether targeting a leap year
	 * @return The length of this month at that kind of year
	 */
	def lengthAt(leapYear: Boolean) = if (isDifferentOnLeapYear) length + 1 else length
}

object Month
{
	// TYPES    -----------------------
	
	/**
	 * A Java version of this enumeration
	 */
	type JMonth = java.time.Month
	
	
	// ATTRIBUTES   -------------------
	
	/**
	 * All 12 months from January to December
	 */
	lazy val values = Vector[Month](January, February, March, April, May, June, July, August, September,
		October, November, December)
	
	
	// IMPLICIT -----------------------
	
	// Implicitly converts from & to Java
	implicit def apply(month: JMonth): Month = apply(month.getValue)
	implicit def toJava(month: Month): JMonth = month.toJava
	
	
	// OTHER    -----------------------
	
	/**
	 * @param month 1-based index of the targeted month
	 * @return Month matching that index
	 */
	@throws[IndexOutOfBoundsException]("If 'month' is < 1 or > 12")
	def apply(month: Int) = values(month - 1)
	
	
	// VALUES   -----------------------
	
	case object January extends Month
	{
		override val value: Int = 1
		override lazy val name: String = "January"
		override val length: Days = Days(31)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => February
			case Negative => December
		}
		override def is(extreme: Extreme): Boolean = extreme == Min
	}
	case object February extends Month
	{
		override val value: Int = 2
		override lazy val name: String = "February"
		override val length: Days = Days(29)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => March
			case Negative => January
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object March extends Month
	{
		override val value: Int = 3
		override lazy val name: String = "March"
		override val length: Days = Days(31)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => April
			case Negative => February
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object April extends Month
	{
		override val value: Int = 4
		override lazy val name: String = "April"
		override val length: Days = Days(30)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => May
			case Negative => March
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object May extends Month
	{
		override val value: Int = 5
		override lazy val name: String = "May"
		override val length: Days = Days(31)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => June
			case Negative => April
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object June extends Month
	{
		override val value: Int = 6
		override lazy val name: String = "June"
		override val length: Days = Days(30)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => July
			case Negative => May
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object July extends Month
	{
		override val value: Int = 7
		override lazy val name: String = "July"
		override val length: Days = Days(31)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => August
			case Negative => June
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object August extends Month
	{
		override val value: Int = 8
		override lazy val name: String = "August"
		override val length: Days = Days(31)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => September
			case Negative => July
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object September extends Month
	{
		override val value: Int = 9
		override lazy val name: String = "September"
		override val length: Days = Days(30)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => October
			case Negative => August
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object October extends Month
	{
		override val value: Int = 10
		override lazy val name: String = "October"
		override val length: Days = Days(31)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => November
			case Negative => September
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object November extends Month
	{
		override val value: Int = 11
		override lazy val name: String = "November"
		override val length: Days = Days(30)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => December
			case Negative => October
		}
		override def is(extreme: Extreme): Boolean = false
	}
	case object December extends Month
	{
		override val value: Int = 12
		override lazy val name: String = "December"
		override val length: Days = Days(31)
		
		override def next(direction: Sign): Month = direction match {
			case Positive => January
			case Negative => November
		}
		override def is(extreme: Extreme): Boolean = extreme == Max
	}
}
