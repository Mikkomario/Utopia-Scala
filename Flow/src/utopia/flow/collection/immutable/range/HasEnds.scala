package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.{Combinable, Sign}
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.math.Ordered.orderingToOrdered

/**
  * A common trait for items which have two ends: A start and an end.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points on this item
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasEnds[P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Ordering used for the end-points of this range
	  */
	implicit def ordering: Ordering[P]
	
	/**
	  * @return The starting point of this span
	  */
	def start: P
	/**
	  * @return The ending point of this range
	  * @see .isInclusive
	  */
	def end: P
	
	/**
	  * @return Whether the end-point of this range is inclusive (true) or exclusive (false)
	  */
	def isInclusive: Boolean
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Whether this is an exclusive range.
	  *         The 'end' in exclusive ranges is not considered to be contained within the range itself.
	  */
	def isExclusive = !isInclusive
	
	/**
	  * @return Whether this range moves from a smaller 'start' into a larger 'end'
	  */
	def isAscending = end >= start
	/**
	  * @return Whether this range moves from a larger 'start' into a smaller 'end'
	  */
	def isDescending = end <= start
	
	/**
	  * @return Positive if values increase along this range, Negative if they decrease
	  */
	def direction: Sign = if (isAscending) Positive else Negative
	
	/**
	  * @return A pair containing the start and end points of this range
	  */
	def toPair = Pair(start, end)
	
	/**
	  * @return Whether this is an empty range
	  */
	def isEmpty = isExclusive && start == end
	/**
	  * @return Whether this range is not empty
	  */
	def nonEmpty = !isEmpty
	
	
	// OTHER    -------------------------
	
	/**
	  * @param point A point
	  * @return Whether that point lies within this range
	  */
	def contains(point: P) = {
		val compares = toPair.map { ordering.compare(point, _).sign }
		if (isInclusive)
			compares.isAsymmetric || compares.contains(0)
		else
			compares.second != 0 && compares.isAsymmetric
	}
	/**
	  * @param other Another range
	  * @return Whether this range contains all items in that range
	  */
	def contains(other: HasEnds[P]): Boolean =
		other.nonEmpty && (contains(other.start) &&
			(contains(other.end) || (isExclusive && other.isExclusive && end == other.end)))
	/**
	  * @param other Another range
	  * @return Whether these two ranges overlap at some point
	  */
	def overlapsWith(other: HasEnds[P]) =
		other.nonEmpty && (contains(other.start) || other.contains(start))
}
