package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.End.{First, Last}
import utopia.flow.operator.Extreme.{Max, Min}
import utopia.flow.operator.{End, Extreme, Sign}
import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasInclusiveEnds[P] extends HasEnds[P]
{
	// COMPUTED -----------------------
	
	/**
	  * @return The minimum and the maximum of this span
	  */
	def minMax: Pair[P] = toPair.sorted
	
	/**
	  * @return Whether this span is of length 1 (i.e. the start and the end overlap)
	  */
	def isUnit = start == end
	
	/**
	  * @return The only value contained within this range.
	  *         None if this range contains multiple values.
	  */
	def only = Some(start).filter { _ == end }
	
	
	// IMPLEMENTED  --------------------
	
	override def isInclusive = true
	
	override def isEmpty = false
	
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
	  * @param end The targeted end of this span
	  * @return The item at the targeted end
	  */
	def apply(end: End) = end match {
		case First => start
		case Last => this.end
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
