package utopia.flow.collection.immutable

import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{Combinable, Signed}

object IterableSpan
{
	// OTHER    --------------------------
	
	/**
	  * Creates a new span
	  * @param start Starting point of this span
	  * @param end Ending point of this span
	  * @param step Step taken at each iteration
	  * @param ordering Implicit ordering to apply
	  * @tparam P Type of points in this span
	  * @tparam D Type of distances in this span
	  * @return A new span that may be iterated
	  */
	def apply[P <: Combinable[D, P], D <: Signed[D]](start: P, end: P, step: D)
	                                                (implicit ordering: Ordering[P]): IterableSpan[P, D] =
		_IterableSpan(start, end, step)
	
	/**
	  * Creates a new numeric span
	  * @param start Starting point of this span
	  * @param end Ending point of this span
	  * @param step Step taken at each iteration
	  * @param n Implicit numeric functions
	  * @tparam P Type of span end-points
	  * @return A new span
	  */
	def numeric[P](start: P, end: P, step: P)(implicit n: Numeric[P]): IterableSpan[P, P] =
		NumericSpan(start, end, step)
	
	
	// NESTED   --------------------------
	
	private case class _IterableSpan[P <: Combinable[D, P], D <: Signed[D]]
	(override val start: P, override val end: P, override val step: D)
	(implicit override val ordering: Ordering[P])
		extends IterableSpan[P, D]
	{
		override protected def signOf(distance: D) = distance.sign
		
		override protected def traverse(from: P, distance: D) = from + distance
		
		override protected def invert(distance: D) = -distance
		
		override protected def withEnds(start: P, end: P) = copy(start, end)
		
		override def by(step: D) = copy(step = step)
	}
	
	private case class NumericSpan[N](override val start: N, override val end: N, override val step: N)
	                                 (implicit n: Numeric[N])
		extends IterableSpan[N, N]
	{
		override def by(step: N) = copy(step = step)
		
		override protected def signOf(distance: N) =
			if (n.lteq(distance, n.zero)) Positive else Negative
		
		override protected def traverse(from: N, distance: N) = n.plus(from, distance)
		
		override protected def invert(distance: N) = n.negate(distance)
		
		override protected def withEnds(start: N, end: N) = copy(start, end)
		
		override implicit def ordering: Ordering[N] = n
	}
}

/**
  * Common trait for items which have inclusive start and end points and which may be iterated like ranges.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait IterableSpan[P, D] extends Span[P] with Spanning[P, D] with SpanLike[P, IterableSpan[P, D]]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @param step Step to apply
	  * @return A copy of this span with that step amount
	  */
	def by(step: D): IterableSpan[P, D]
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def _repr = this
}
