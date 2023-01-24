package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Sign
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
		implicit val ord: Ordering[P] = ordering
		val _ends = minMax
		if (ordering.lt(point, _ends.first))
			_ends.first
		else if (ordering.gt(point, _ends.second))
			_ends.second
		else
			point
	}
	
	/**
	 * @param side The targeted side, where negative is the start and positive is the end
	 * @return The targeted end of this span
	 */
	def endAt(side: Sign) = side match {
		case Negative => start
		case Positive => end
	}
}
