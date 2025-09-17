package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.End

object HasInclusiveEnds
{
	// OTHER    ------------------------
	
	/**
	  * @param start Range starting point (inclusive)
	  * @param end Range ending point (inclusive)
	  * @tparam P Type of range endpoints
	  * @return A new range between those two points
	  */
	def apply[P](start: P, end: P): HasInclusiveEnds[P] = _HasInclusiveEnds(start, end)
	/**
	  * @param ends The range start and end point (inclusive)
	  * @tparam P Type of range endpoints
	  * @return A new range between the specified points
	  */
	def apply[P](ends: Pair[P]): HasInclusiveEnds[P] = PairHasInclusiveEnds(ends)
	
	
	// NESTED   ------------------------
	
	private case class _HasInclusiveEnds[+P](start: P, end: P) extends HasInclusiveEnds[P]
	{
		override lazy val ends = super.ends
	}
	private case class PairHasInclusiveEnds[+P](override val ends: Pair[P]) extends HasInclusiveEnds[P]
	{
		override def start: P = ends.first
		override def end: P = ends.second
	}
}

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasInclusiveEnds[+P] extends HasEnds[P] with MayHaveInclusiveEnds[P]
{
	// COMPUTED ------------------------
	
	/**
	 * @param ordering Implicit ordering to apply
	 * @tparam P2 Type of the ordered end-points
	 * @return A span with these ends
	 */
	def toSpan[P2 >: P](implicit ordering: Ordering[P2]): Span[P2] = Span.from(this)
	/**
	 * @param n Implicit numeric implementation
	 * @return A numeric span matching this range
	 */
	def toNumericSpan[P2 >: P](implicit n: Numeric[P2]): NumericSpan[P2] = NumericSpan.from(this)
	
	
	// IMPLEMENTED  --------------------
	
	override def isUnit = start == end
	
	
	// OTHER    ----------------------
	
	/**
	  * @param end The targeted end of this span
	  * @return The item at the targeted end
	  */
	def apply(end: End) = end match {
		case First => start
		case Last => this.end
	}
}
