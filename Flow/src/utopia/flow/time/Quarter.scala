package utopia.flow.time

import utopia.flow.operator.Steppable
import utopia.flow.operator.combine.{Combinable, Subtractable}
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.Month.{April, January, July, October}

/**
 * An enumeration for yearly quarters (Q1, Q2, Q3 and Q4)
 * @author Mikko Hilpinen
 * @since 20.11.2021, v1.14.1
 */
sealed trait Quarter
	extends SelfComparable[Quarter] with Steppable[Quarter]
		with Combinable[Int, Quarter] with Subtractable[Int, Quarter]
{
	// ATTRIBUTES   -------------------
	
	/**
	  * @return A 0-based index of this quarter
	  */
	def index: Int
	/**
	 * @return The first month of this quarter
	 */
	def firstMonth: Month
	/**
	 * @return The quarter following this one
	 */
	def next: Quarter
	/**
	 * @return The quarter previous to this one
	 */
	def previous: Quarter
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return Last month of this quarter (inclusive)
	 */
	def lastMonth = firstMonth + 2
	/**
	 * @return The ending month of this quarter (exclusive)
	 */
	def endMonth = firstMonth + 3
	/**
	 * @return The months in this quarter
	 */
	def months = Vector(firstMonth, firstMonth.next, lastMonth)
	
	/**
	 * @return The first day of this quarter (inclusive)
	 */
	def firstDay = firstMonth.firstDay
	/**
	 * @return Yearly dates that belong to this quarter
	 */
	def dates = YearlyDateRange.exclusive(firstDay, endMonth.firstDay)
	
	
	// IMPLEMENTED  ------------------
	
	override def self = this
	
	override def next(direction: Sign): Quarter = direction match {
		case Positive => next
		case Negative => previous
	}
	
	override def +(other: Int) = {
		val newIndex = (index + other) % 4
		if (newIndex < 0)
			Quarter.values(4 + newIndex)
		else
			Quarter(newIndex)
	}
	override def -(other: Int) = this + (-other)
	
	override def compareTo(o: Quarter) = index.compareTo(o.index)
	
	
	// OTHER    ----------------------
	
	/**
	 * @param year Targeted year (year)
	 * @return Dates that belong to this quarter on that year
	 */
	def datesAt(year: Year) = dates.at(year).head
	/**
	 * @param year Targeted year (int)
	 * @return Dates that belong to this quarter on that year
	 */
	def datesAtYear(year: Int) = dates.atYear(year).head
	
	/**
	 * @param month A month
	 * @return Whether this quarter contains that month
	 */
	def contains(month: Month) = month >= firstMonth && month <= lastMonth
	/**
	 * @param day A month day
	 * @return Whether this quarter contains that day
	 */
	def contains(day: MonthDay): Boolean = contains(day.month)
}

object Quarter
{
	// ATTRIBUTES   -------------------
	
	/**
	 * All four quarters in order
	 */
	val values = Vector[Quarter](Q1, Q2, Q3, Q4)
	
	
	// OTHER    -----------------------
	
	/**
	 * @param index Quarter index [1-4]
	 * @return Quarter with that index
	 */
	def apply(index: Int) = values((index - 1) % 4)
	
	/**
	 * @param monthIndex Targeted month index [1-12], starting from January
	 * @return Quarter that contains that month
	 */
	def withMonth(monthIndex: Int) = values((monthIndex / 3) - 1)
	/**
	 * @param month Targeted month
	 * @return Quarter that contains that month
	 */
	def containing(month: Month) = withMonth(month.value)
	
	
	// NESTED   -----------------------
	
	/**
	 * The first yearly quarter
	 */
	case object Q1 extends Quarter
	{
		override val index: Int = 0
		override val firstMonth = January
		
		override def next = Q2
		override def previous = Q4
		
		override def is(extreme: Extreme): Boolean = extreme == Min
	}
	/**
	 * The second yearly quarter
	 */
	case object Q2 extends Quarter
	{
		override val index: Int = 1
		override val firstMonth = April
		
		override def next = Q3
		override def previous = Q1
		
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	 * The third yearly quarter
	 */
	case object Q3 extends Quarter
	{
		override val index: Int = 2
		override val firstMonth = July
		
		override def next = Q4
		override def previous = Q2
		
		override def is(extreme: Extreme): Boolean = false
	}
	/**
	 * The fourth yearly quarter
	 */
	case object Q4 extends Quarter
	{
		override val index: Int = 3
		override val firstMonth = October
		
		override def next = Q1
		override def previous = Q3
		
		override def is(extreme: Extreme): Boolean = extreme == Max
	}
}
