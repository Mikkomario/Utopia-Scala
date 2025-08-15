package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}

import scala.math.Ordered.orderingToOrdered

/**
  * A common trait for items which have 0-2 ends. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 15.8.2025, v2.7
  */
trait MayHaveInclusiveOrderedEnds[P] extends MayHaveOrderedEnds[P] with MayHaveInclusiveEnds[P]
{
	// COMPUTED  --------------------
	
	/**
	 * @return The minimum end of this range, followed by the maximum end of this range.
	 *         Contains None in cases where the end is open / undefined.
	 */
	def minAndMaxOption = if (isAscending) endOptions else Pair(endOption, startOption)
	
	/**
	 * @throws UnsupportedOperationException If this range has an infinitely small minimum
	 * @return The smallest defined value on this range
	 */
	@throws[UnsupportedOperationException]("If this range has an infinitely small minimum")
	def min = minOption.getOrElse { throw new UnsupportedOperationException("min called on an infinite range") }
	/**
	 * @return The smallest defined value on this range. None if this range has no defined minimum point.
	 */
	def minOption = if (isAscending) startOption else endOption
	
	/**
	 * @throws UnsupportedOperationException If this range has an infinitely large maximum
	 * @return The largest defined value on this range
	 */
	@throws[UnsupportedOperationException]("If this range has an infinitely large maximum")
	def max = maxOption.getOrElse { throw new UnsupportedOperationException("min called on an infinite range") }
	/**
	 * @return The largest defined value on this range. None if this range has no defined maximum point.
	 */
	def maxOption = if (isAscending) endOption else startOption
	
	
	// OTHER    ----------------------
	
	/**
	  * @param point A point
	  * @return A copy of that point that has been restricted to this span.
	  *         The point is adjusted as little as possible.
	  */
	def restrict(point: P) = minOption.filter { _ > point }.getOrElse {
		maxOption.filter { _ < point }.getOrElse(point)
	}
	
	/**
	  * @param extreme Targeted extreme (min or max)
	  * @return The most extreme value in this range.
	 *         None if this range has no specified extreme, but is infinite towards that direction.
	  */
	def get(extreme: Extreme) = extreme match {
		case Max => maxOption
		case Min => minOption
	}
}
