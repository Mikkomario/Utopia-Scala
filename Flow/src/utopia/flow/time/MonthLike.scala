package utopia.flow.time

import utopia.flow.operator.Steppable
import utopia.flow.operator.combine.{Combinable, Subtractable}
import utopia.flow.operator.ordering.SelfComparable
import utopia.flow.operator.sign.Sign.{Negative, Positive}

import scala.language.implicitConversions

/**
 * Common trait for month representations (i.e. both months and year months)
 * @author Mikko Hilpinen
 * @since 04.06.2025, v2.7
 */
trait MonthLike[Repr <: Steppable[Repr]]
	extends SelfComparable[Repr] with Combinable[Int, Repr] with Subtractable[Int, Repr] with Steppable[Repr]
{
	// ABSTRACT ----------------------
	
	/**
	 * @return Name of this month in English
	 */
	def name: String
	/**
	 * @return The normal length of this month.
	 *         Note: Does not account for leap years.
	 */
	def length: Days
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return The month following this one
	 */
	def next: Repr = next(Positive)
	/**
	 * @return The month previous to this one
	 */
	def previous: Repr = next(Negative)
}


