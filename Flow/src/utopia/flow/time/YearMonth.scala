package utopia.flow.time

import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Month.{December, January}

/**
 * Represents a month at a specific year
 *
 * @author Mikko Hilpinen
 * @since 04.06.2025, v2.7
 */
case class YearMonth(year: Year, month: Month) extends MonthLike[YearMonth]
{
	override def self = this
	
	override def name = month.name
	override def length = month.lengthAt(year)
	
	override def +(other: Int) = {
		var year = this.year
		var months = month.value + other
		
		while (months < 1) {
			year = year.previous
			months += 12
		}
		while (months > 12) {
			year = year.next
			months -= 12
		}
		
		YearMonth(year, Month(months))
	}
	override def -(other: Int) = this + (-other)
	override def compareTo(o: YearMonth) = {
		if (year == o.year)
			month.compareTo(o.month)
		else
			year.compareTo(o.year)
	}
	
	override def next(direction: Sign) = month.nextOption(direction) match {
		case Some(nextMonth) => copy(month = nextMonth)
		case None =>
			direction match {
				case Positive => YearMonth(year.next, January)
				case Negative => YearMonth(year.previous, December)
			}
	}
	override def is(extreme: Extreme) = false
}
