package utopia.flow.collection.immutable.range

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.flow.operator.enumeration.{End, Extreme}
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

import scala.math.Ordering.Implicits.infixOrderingOps

/**
  * A common trait for items which have 0-2 comparable ends, like ranges, and which may be copied and modified in
  * an immutable fashion.
  * @author Mikko Hilpinen
  * @since 15.8.2025, v2.7
  */
trait OpenRangeLike[P, +Repr, +Open] extends MayHaveInclusiveOrderedEnds[P]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return "This" element
	  */
	protected def self: Repr
	
	/**
	 * @return A reverse of this span, where the start and end are swapped
	 */
	def reverse: Repr
	
	/**
	 * @param start A new starting point for this span
	 * @return A copy of this span with the new starting point
	 */
	def withStart(start: P): Repr
	/**
	 * @param end A new end-point for this span
	 * @return A copy of this span with the new ending point
	 */
	def withEnd(end: P): Repr
	/**
	 * Creates a copy of this element
	 * @param start New starting point
	 * @param end New ending point
	 * @return A copy of this element with the specified start and end points
	 */
	def withEnds(start: P, end: P): Repr
	/**
	 * @param start A new starting point. None if open.
	 * @param end A new ending point. None if open.
	 * @return A copy of this element with the specified start & end points
	 */
	def withPossibleEnds(start: Option[P], end: Option[P]): Open
	
	/**
	 * @param min A new minimum point for this span
	 * @return A copy of this span with that minimum point
	 */
	def withMin(min: P): Repr
	/**
	 * @param max A new maximum point for this span
	 * @return A copy of this span with that maximum point
	 */
	def withMax(max: P): Repr
	
	/**
	 * @param f A mapping function to apply to the starting value of this span
	 * @return Copy of this span with mapped starting value
	 */
	def mapStart(f: P => P): Repr
	/**
	 * @param f A mapping function to apply to the final value of this span
	 * @return Copy of this span with mapped final value
	 */
	def mapEnd(f: P => P): Repr
	/**
	 * @param f A mapping function to apply to the end-points of this span
	 * @return A copy of this span with mapped end-points
	 */
	def mapEnds(f: P => P): Repr
	
	/**
	 * @param point A point to include within this span
	 * @return A copy of this span that is extended to include the specified point
	 */
	def including(point: P): Repr
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this span where the 'start' is smaller than the 'end'
	  */
	def ascending = if (isAscending) self else reverse
	/**
	  * @return A copy of this span where the 'start' is larger than the 'end'
	  */
	def descending = if (isDescending) self else reverse
	
	/**
	 * @return A copy of this range with open start
	 */
	def withoutStart = withPossibleEnds(None, endOption)
	/**
	 * @return A copy of this range with open end
	 */
	def withoutEnd = withPossibleEnds(startOption, None)
	
	/**
	 * @return A copy of this range with an infinitely small minimum value / no minimum
	 */
	def withoutMin = if (isAscending) withoutStart else withoutEnd
	/**
	 * @return A copy of this range with an infinitely large maximum value / no maximum
	 */
	def withoutMax = if (isAscending) withoutEnd else withoutStart
	
	
	// OTHER    -----------------------
	
	/*
	 * Maps the ends of this span to a different data type
	 * @param f A mapping function applied for both ends of this span
	 * @param ord Ordering to use for the mapping results
	 * @tparam P2 Type of mapping results
	 * @return A new span based on the mapping result of this span
	 */
	// def mapTo[P2](f: P => P2)(implicit ord: Ordering[P2]) = ???
	
	/**
	  * @param end New start or end for this span
	  * @param side Whether to replace the start point (First) or the end point (Last)
	  * @return Copy of this span with the targeted end replaced
	  */
	def withEnd(end: P, side: End): Repr = side match {
		case First => withStart(end)
		case Last => withEnd(end)
	}
	/**
	 * @param value New minimum or maximum value for this span
	 * @param extreme Targeted / replaced extreme
	 * @return Copy of this span with the replaced minimum or maximum value
	 */
	def withExtreme(value: P, extreme: Extreme) = extreme match {
		case Min => withMin(value)
		case Max => withMax(value)
	}
	
	/**
	 * @param end End to open / remove
	 * @return A copy of this range with the specified end opened
	 */
	def without(end: End) = end match {
		case First => withoutStart
		case Last => withoutEnd
	}
	/**
	 * @param extreme The extreme to open / remove
	 * @return A copy of this range without a defined endpoint at the specified extreme
	 */
	def without(extreme: Extreme) = extreme match {
		case Min => withoutMin
		case Max => withoutMax
	}
	
	/**
	  * @param end Targeted end (start (First) or end (Last))
	  * @param f A mapping function to apply to the targeted value of this span
	  * @return Copy of this span with a mapped value
	  */
	def mapSpecificEnd(end: End)(f: P => P) = end match {
		case First => mapStart(f)
		case Last => mapEnd(f)
	}
	/**
	 * @param f A mapping function to apply to the end-points of this span.
	 *          Receives None in case of undefined / open ends.
	 * @return A copy of this span with mapped end-points
	 */
	def mapPossibleEnds(f: Option[P] => Option[P]): Open = withPossibleEnds(f(startOption), f(endOption))
	
	/**
	  * @param direction A direction (positive (i.e. ascending) or negative (i.e. descending))
	  * @return A copy of this span with that direction
	  */
	def withDirection(direction: Sign) = if (this.direction == direction) self else reverse
	/**
	  * @param extreme The extreme that should be placed at the end of this span
	  * @return Copy of this span where the more extreme value is at the end
	  */
	def towards(extreme: Extreme) = extreme match {
		case Max => ascending
		case Min => descending
	}
	
	/**
	  * @param points A set of points
	  * @return A copy of this span that is extended to include all of the specified points
	  */
	def including(points: IterableOnce[P]): Repr = points match {
		case span: HasInclusiveOrderedEnds[P] => _including(span.minMax)
		case i: Iterable[P] =>
			if (i.knownSize == 1)
				including(i.head)
			else
				i.minMaxOption match {
					case Some(minMax) => _including(minMax)
					case None => self
				}
		case o =>
			o.iterator.minMaxOption match {
				case Some(minMax) => _including(minMax)
				case None => self
			}
	}
	/**
	  * @param p1 A point
	  * @param p2 Another point
	  * @param more More points
	  * @return A copy of this span that is extended to include all of the specified points
	  */
	def including(p1: P, p2: P, more: P*): Repr = including(Set(p1, p2) ++ more)
	
	/**
	  * @param distance A distance to move this span
	  * @param plus A function that combines a point and a distance
	  * @return A copy of this span where both the start and the end points have been moved the specified distance
	  */
	protected def _shiftedBy[D](distance: D)(plus: (P, D) => P) = mapEnds { plus(_, distance) }
	
	private def _including(minMax: Pair[P]) = {
		val myMinMax = minAndMaxOption
		val newMin = myMinMax.first match {
			case Some(my) => my min minMax.first
			case None => minMax.first
		}
		val newMax = myMinMax.second match {
			case Some(my) => my max minMax.second
			case None => minMax.second
		}
		direction match {
			case Positive => withEnds(newMin, newMax)
			case Negative => withEnds(newMax, newMin)
		}
	}
}
