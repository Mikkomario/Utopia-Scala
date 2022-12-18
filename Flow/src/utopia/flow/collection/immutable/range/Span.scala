package utopia.flow.collection.immutable.range

object Span
{
	// OTHER    -------------------------
	
	/**
	  * @param start Starting point
	  * @param end Ending point (inclusive)
	  * @param ordering Implicit ordering to apply
	  * @tparam P Type or points covered by this span
	  * @return A new span that covers the area from 'start' to 'end'
	  */
	def apply[P](start: P, end: P)(implicit ordering: Ordering[P]): Span[P] = _Span[P](start, end)
	
	
	// NESTED   -------------------------
	
	private case class _Span[P](override val start: P, override val end: P)
	                           (implicit override val ordering: Ordering[P])
		extends Span[P]
	{
		override protected def withEnds(start: P, end: P) = copy(start, end)
	}
}

/**
  * Common trait for items which have two inclusive end-points: a start and an end
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait Span[P] extends SpanLike[P, Span[P]]
{
	override protected def self = this
}
