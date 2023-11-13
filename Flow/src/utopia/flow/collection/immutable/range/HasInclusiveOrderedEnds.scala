package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.Sign

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasInclusiveOrderedEnds[P] extends HasOrderedEnds[P] with HasInclusiveEnds[P]
{
	// COMPUTED -----------------------
	
	/**
	  * @return The minimum and the maximum of this span
	  */
	def minMax: Pair[P] = ends.sorted
	
	
	// IMPLEMENTED  --------------------
	
	def min[B >: P](implicit ord: Ordering[B]) = ord.min(start, end)
	def minOption[B >: P](implicit ord: Ordering[B]) = Some(min)
	
	def max[B >: P](implicit ord: Ordering[B]) = ord.max(start, end)
	def maxOption[B >: P](implicit ord: Ordering[B]) = Some(max)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param point A point
	  * @return A copy of that point that has been restricted to this span.
	  *         The point is adjusted as little as possible.
	  */
	def restrict(point: P) = {
		val _ends = minMax
		if (ordering.lt(point, _ends.first))
			_ends.first
		else if (ordering.gt(point, _ends.second))
			_ends.second
		else
			point
	}
	
	/**
	  * @param extreme Targeted extreme (min or max)
	  * @return The most extreme value at the specified end
	  */
	def apply(extreme: Extreme) = extreme match {
		case Max => ordering.max(start, end)
		case Min => ordering.min(start, end)
	}
	/**
	 * @param side The targeted side, where negative is the start and positive is the end
	 * @return The targeted end of this span
	 */
	@deprecated("Replaced with .apply(End)", "v2.0")
	def endAt(side: Sign) = side match {
		case Negative => start
		case Positive => end
	}
}
