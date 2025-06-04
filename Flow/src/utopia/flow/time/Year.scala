package utopia.flow.time

import utopia.flow.operator.{Reversible, Steppable}
import utopia.flow.operator.combine.{Combinable, Subtractable}
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign

import java.time
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
	 * @return A Java model of this year
	 */
	def toJava = time.Year.of(value)
	
	
	// IMPLEMENTED  --------------------------
	
	override def self = this
	
	override def unary_- : Year = Year(-value)
	
	override def +(other: Int): Year = Year(value + other)
	override def -(years: Int) = Year(value - years)
	override def compareTo(o: Year) = value.compareTo(o.value)
	
	override def next(direction: Sign): Year = Year(value + direction.modifier)
	override def is(extreme: Extreme): Boolean = false
}
