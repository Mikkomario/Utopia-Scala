package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair

import scala.language.implicitConversions

object Span
{
	// IMPLICIT -------------------------
	
	/**
	 * @param range An ordered range
	 * @tparam P Type of the range end-points
	 * @return A span from the specified range
	 */
	implicit def from[P](range: HasInclusiveOrderedEnds[P]): Span[P] = range match {
		case s: Span[P] => s
		case r => apply(r.start, r.end)(r.ordering)
	}
	/**
	 * @param range A range
	 * @param ordering Implicit ordering to use, but only if the specified range doesn't provide one
	 * @tparam P Type of range end-points
	 * @return A span from the specified range
	 */
	implicit def from[P](range: HasInclusiveEnds[P])(implicit ordering: Ordering[P]): Span[P] = range match {
		case s: Span[P] => s
		case r: HasInclusiveOrderedEnds[P] => from(r)
		case r => apply(r.start, r.end)
	}
	
	
	// OTHER    -------------------------
	
	/**
	  * @param start Starting point
	  * @param end Ending point (inclusive)
	  * @param ordering Implicit ordering to apply
	  * @tparam P Type or points covered by this span
	  * @return A new span that covers the area from 'start' to 'end'
	  */
	def apply[P](start: P, end: P)(implicit ordering: Ordering[P]): Span[P] = _Span[P](start, end)
	/**
	  * @param ends Start and end point (inclusive)
	  * @param ordering Implicit ordering to apply
	  * @tparam P Type or points covered by this span
	  * @return A new span that covers the area between the specified endpoints*
	  */
	def apply[P](ends: Pair[P])(implicit ordering: Ordering[P]): Span[P] = apply(ends.first, ends.second)
	
	/**
	  * @param value The singular value to wrap
	  * @param ordering Ordering to use
	  * @tparam P Type of the specified value
	  * @return A span from the specified value to the specified value
	  */
	def singleValue[P](value: P)(implicit ordering: Ordering[P]) = apply(value, value)
	
	/**
	 * @param start Starting point
	 * @param end Ending point
	 * @param n Numeric implementation for the end-points
	 * @tparam N Type of numeric type used
	 * @return A new span
	 */
	def numeric[N](start: N, end: N)(implicit n: Numeric[N]) = NumericSpan(start, end)
	
	
	// NESTED   -------------------------
	
	private case class _Span[P](override val start: P, override val end: P)
	                           (implicit override val ordering: Ordering[P])
		extends Span[P]
	{
		override protected def self: Span[P] = this
		
		override def withEnds(start: P, end: P) = copy(start, end)
	}
}

/**
  * Common trait for items which have two inclusive end-points: a start and an end
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait Span[P] extends OpenRange[P] with SpanLike[P, Span[P]]
{
	override def toString = s"$start to $end"
}
