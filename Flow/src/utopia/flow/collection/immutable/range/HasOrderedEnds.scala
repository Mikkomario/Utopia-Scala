package utopia.flow.collection.immutable.range

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
	
	/**
	 * @param other An instance that has ends
	 * @param ord Implicit ordering to use (if 'other' is not ordered)
	 * @tparam P Type of end points
	 * @return An ordered range, based on the specified instance
	 */
	def from[P](other: HasEnds[P])(implicit ord: Ordering[P]) = other match {
		case p: HasOrderedEnds[P] => p
		case o => apply(o.start, o.end, exclusive = o.isExclusive)
	}
	/**
	 * Converts a [[Range]]
	 * @param range Range to convert
	 * @return A HasOrderedEnds based on the specified range
	 */
	def from(range: Range) = apply(range.start, range.end, exclusive = !range.isInclusive)
	
	
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
trait HasOrderedEnds[P] extends HasEnds[P] with MayHaveOrderedEnds[P]
{
	// IMPLEMENTED  ---------------------
	
	override def isAscending = end >= start
	override def isDescending = end <= start
	
	override def contains(point: P) = {
		val compares = ends.map { ordering.compare(point, _).sign }
		if (isInclusive)
			compares.isAsymmetric || compares.contains(0)
		else
			compares.second != 0 && compares.isAsymmetric
	}
	override def contains(other: HasEnds[P]): Boolean =
		other.nonEmpty && (contains(other.start) &&
			(contains(other.end) || (isExclusive && other.isExclusive && end == other.end)))
	
	
	// OTHER    -------------------------
	
	/**
	  * @param other Another range
	  * @return Whether these two ranges overlap at some point
	  */
	def overlapsWith(other: HasOrderedEnds[P]) =
		other.nonEmpty && (contains(other.start) || other.contains(start))
}
