package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}

import scala.language.implicitConversions

object HasInclusiveOrderedEnds
{
	// IMPLICIT ----------------------------
	
	/**
	 * Makes sure a range is ordered
	 *
	 * @param ends     A range
	 * @param ordering Implicit ordering to apply, but only if the specified range doesn't provide one
	 * @tparam P Type of the range end-points
	 * @return An ordered copy of the specified range
	 */
	implicit def from[P](ends: HasInclusiveEnds[P])(implicit ordering: Ordering[P]): HasInclusiveOrderedEnds[P] =
		ends match {
			case ordered: HasInclusiveOrderedEnds[P] => ordered
			case range => apply(ends.start, ends.end)
		}
	
	
	// OTHER    ---------------------------
	
	def apply[P](start: P, end: P)(implicit ordering: Ordering[P]): HasInclusiveOrderedEnds[P] =
		_HasInclusiveOrderedEnds(start, end, ordering)
	
	
	// NESTED   ---------------------------
	
	private case class _HasInclusiveOrderedEnds[P](start: P, end: P, ordering: Ordering[P])
		extends HasInclusiveOrderedEnds[P]
}

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasInclusiveOrderedEnds[P] extends HasOrderedEnds[P] with HasInclusiveEnds[P] with MayHaveInclusiveOrderedEnds[P]
{
	// COMPUTED -----------------------
	
	/**
	  * @return The minimum and the maximum of this span
	  */
	def minMax: Pair[P] = ends.sorted
	
	/**
	 * @return A span matching this range
	 */
	def toSpan: Span[P] = Span.from(this)
	
	
	// IMPLEMENTED  --------------------
	
	override def min = ordering.min(start, end)
	override def minOption = Some(min)
	
	override def max = ordering.max(start, end)
	override def maxOption = Some(max)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param extreme Targeted extreme (min or max)
	  * @return The most extreme value at the specified end
	  */
	def apply(extreme: Extreme) = extreme match {
		case Max => max
		case Min => min
	}
}
