package utopia.flow.collection.immutable.range

import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

import scala.math.Ordered.orderingToOrdered


/**
  * A common trait for items which have 0-2 linearly ordered ends.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points on this item
  * @author Mikko Hilpinen
  * @since 15.8.2025, v2.7
  */
trait MayHaveOrderedEnds[P] extends MayHaveEnds[P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Ordering used for the end-points of this range
	  */
	implicit def ordering: Ordering[P]
	
	/**
	 * @return Whether this range moves from a smaller 'start' into a larger 'end'
	 */
	def isAscending: Boolean
	/**
	 * @return Whether this range moves from a larger 'start' into a smaller 'end'
	 */
	def isDescending: Boolean
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return Positive if values increase along this range, Negative if they decrease
	  */
	def direction: Sign = if (isAscending) Positive else Negative
	
	
	// OTHER    -------------------------
	
	/**
	  * @param point A point
	  * @return Whether that point lies within this range
	  */
	def contains(point: P) = {
		// Case: Empty range => Can't contain values
		if (isEmpty)
			false
		else if (isAscending)
			startOption.forall { _ <= point } &&
				endOption.forall { end => if (isInclusive) end >= point else end > point }
		else
			startOption.forall { _ >= point } &&
				endOption.forall { end => if (isInclusive) end <= point else end < point }
	}
	/**
	  * @param other Another range
	  * @return Whether this range contains all items in that range
	  */
	def contains(other: HasEnds[P]): Boolean =
		other.nonEmpty && (contains(other.start) &&
			(contains(other.end) || (isExclusive && other.isExclusive && endOption.contains(other.end))))
}
