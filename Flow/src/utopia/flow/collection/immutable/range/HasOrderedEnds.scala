package utopia.flow.collection.immutable.range

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.math.Ordered.orderingToOrdered

object HasOrderedEnds
{
	// OTHER    ------------------------
	
	/**
	  * Creates a new range
	  * @param start First value (inclusive)
	  * @param end Ending value (inclusive or exclusive)
	  * @param exclusive Whether the ending value is exclusive (default = false)
	  * @param ord Implicit ordering to apply
	  * @tparam P Type of range end values
	  * @return A new range
	  */
	def apply[P](start: P, end: P, exclusive: Boolean = false)(implicit ord: Ordering[P]): HasOrderedEnds[P] =
		_HasOrderedEnds[P](start, end, !exclusive)
	
	/**
	  * Creates a new inclusive range
	  * @param start     First value (inclusive)
	  * @param end       Ending value (inclusive)
	  * @param ord       Implicit ordering to apply
	  * @tparam P Type of range end values
	  * @return A new range
	  */
	def inclusive[P](start: P, end: P)(implicit ord: Ordering[P]) = apply(start, end)
	/**
	  * Creates a new exclusive range
	  * @param start     First value (inclusive)
	  * @param end       Ending value (exclusive)
	  * @param ord       Implicit ordering to apply
	  * @tparam P Type of range end values
	  * @return A new range
	  */
	def exclusive[P](start: P, end: P)(implicit ord: Ordering[P]) = apply(start, end, exclusive = true)
	
	
	// NESTED   ------------------------
	
	private case class _HasOrderedEnds[P](start: P, end: P, isInclusive: Boolean)
	                                     (implicit override val ordering: Ordering[P])
		extends HasOrderedEnds[P]
}

/**
  * A common trait for items which have two linearly ordered ends: A start and an end.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points on this item
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasOrderedEnds[P] extends HasEnds[P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Ordering used for the end-points of this range
	  */
	implicit def ordering: Ordering[P]
	
	
	// COMPUTED -----------------------
	
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
	
	
	// IMPLEMENTED  ---------------------
	
	override def toString = if (isInclusive) s"$start to $end" else s"$start until $end"
	
	
	// OTHER    -------------------------
	
	/**
	  * @param point A point
	  * @return Whether that point lies within this range
	  */
	def contains(point: P) = {
		val compares = ends.map { ordering.compare(point, _).sign }
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
	def overlapsWith(other: HasOrderedEnds[P]) =
		other.nonEmpty && (contains(other.start) || other.contains(start))
}
