package utopia.flow.time

import scala.language.implicitConversions
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.ordering.RichComparable
import utopia.flow.time.TimeExtensions.ExtendedLocalDate

import java.time.{LocalDate, Period}

/**
  * An object that always resolves to the current date
  * @author Mikko Hilpinen
  * @since 16.3.2021, v1.9
  */
object Today extends ValueConvertible with RichComparable[LocalDate]
{
	// IMPLICIT ----------------------------
	
	implicit def currentDate(today: Today.type): LocalDate = today.toLocalDate
	implicit def extendedDate(today: Today.type): ExtendedLocalDate = today.toLocalDate
	
	
	// COMPUTED  ---------------------------
	
	/**
	  * @return Today as a date
	  */
	def toLocalDate: LocalDate = LocalDate.now()
	
	/**
	  * @return The current year
	  */
	def year: Year = java.time.Year.now()
	/**
	  * @return The current month
	  */
	def month: Month = toLocalDate.month
	/**
	  * @return The current year-month
	  */
	def yearMonth: YearMonth = java.time.YearMonth.now()
	
	
	// IMPLEMENTED  ------------------------
	
	override def toString = toLocalDate.toString
	
	override implicit def toValue: Value = toLocalDate
	
	override def compareTo(o: LocalDate) = toLocalDate.compareTo(o)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param period A time period (days)
	 * @return This date advanced by that time period
	 */
	def +(period: Period) = toLocalDate + period
	/**
	 * @param period A time period (days)
	 * @return A date 'period' before this date
	 */
	def -(period: Period) = toLocalDate - period
	
	def +(days: Days) = toLocalDate + days
	def -(days: Days) = toLocalDate - days
}
