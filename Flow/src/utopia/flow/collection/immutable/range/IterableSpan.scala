package utopia.flow.collection.immutable.range

import utopia.flow.operator.{Combinable, Sign, Signed}

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
	                                                (implicit ordering: Ordering[P]): IterableSpan[P] =
		iterate[P](start, end) { (p, s) => p + step.withSign(s) }
	
	/**
	  * Creates a new span
	  * @param start    Starting point of this span
	  * @param end      Ending point of this span
	  * @param step     A function that advances one step towards the specified direction.
	  *                 Accepts:
	  *                     1) Step origin, and
	  *                     2) Step direction (binary)
	  *
	  *                 Returns the next position along this span.
	  *
	  * @param ord Implicit ordering to apply
	  * @tparam P Type of points in this span
	  * @return A new span that may be iterated
	  */
	def iterate[P](start: P, end: P)(step: (P, Sign) => P)(implicit ord: Ordering[P]): IterableSpan[P] =
		_IterableSpan(start, end)(step)
	
	
	// NESTED   --------------------------
	
	private case class _IterableSpan[P](override val start: P, override val end: P)(advance: (P, Sign) => P)
	                                   (implicit override val ordering: Ordering[P])
		extends IterableSpan[P]
	{
		override def withEnds(start: P, end: P) = copy(start, end)(advance)
		
		override protected def traverse(from: P, direction: Sign) = advance(from, direction)
	}
}

/**
  * Common trait for items which have inclusive start and end points and which may be iterated like ranges.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait IterableSpan[P] extends Span[P] with IterableHasInclusiveEnds[P] with SpanLike[P, IterableSpan[P]]
{
	// IMPLEMENTED  -------------------------
	
	override protected def self = this
	
	override def toString = super[SpanLike].toString
}
