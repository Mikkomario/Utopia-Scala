package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.math.Ordered.orderingToOrdered

/**
  * A common trait for items which have two comparable ends, like ranges, and which may be copied and modified in
  * an immutable fashion.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait SpanLike[P, +Repr] extends HasInclusiveEnds[P]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return "This" element
	  */
	protected def _repr: Repr
	
	/**
	  * Creates a copy of this element
	  * @param start New starting point (default = current start)
	  * @param end New ending point (default = current end)
	  * @return A copy of this element with the specified start and end points
	  */
	protected def withEnds(start: P = this.start, end: P = this.end): Repr

	
	// COMPUTED ------------------------
	
	/**
	  * @return A reverse of this span, where the start and end are swapped
	  */
	def reverse = withEnds(end, start)
	
	/**
	  * @return A copy of this span where the 'start' is smaller than the 'end'
	  */
	def ascending = if (isAscending) _repr else reverse
	/**
	  * @return A copy of this span where the 'start' is larger than the 'end'
	  */
	def descending = if (isDescending) _repr else reverse
	
	
	// OTHER    -----------------------
	
	/**
	  * @param start A new starting point for this span
	  * @return A copy of this span with the new starting point
	  */
	def withStart(start: P) = withEnds(start)
	/**
	  * @param end A new end-point for this span
	  * @return A copy of this span with the new ending point
	  */
	def withEnd(end: P) = withEnds(end = end)
	
	/**
	  * @param min A new minimum point for this span
	  * @return A copy of this span with that minimum point
	  */
	def withMin(min: P) = {
		val _max = max
		if (_max < min)
			withEnds(min, min)
		else if (isAscending)
			withEnds(min, _max)
		else
			withEnds(_max, min)
	}
	/**
	  * @param max A new maximum point for this span
	  * @return A copy of this span with that maximum point
	  */
	def withMax(max: P) = {
		val _min = min
		if (max < _min)
			withEnds(max, max)
		else if (isAscending)
			withEnds(_min, max)
		else
			withEnds(max, _min)
	}
	
	/**
	  * @param direction A direction (positive (i.e. ascending) or negative (i.e. descending))
	  * @return A copy of this span with that direction
	  */
	def withDirection(direction: Sign) = if (this.direction == direction) _repr else reverse
	
	/**
	  * @param point A point to include within this span
	  * @return A copy of this span that is extended to include the specified point
	  */
	def including(point: P) = {
		val _ends = minMax
		if (point < _ends.first)
			withMin(point)
		else if (point > _ends.second)
			withMax(point)
		else
			_repr
	}
	/**
	  * @param points A set of points
	  * @return A copy of this span that is extended to include all of the specified points
	  */
	def including(points: IterableOnce[P]): Repr = points match {
		case span: Spanning[P, _] => _including(span.minMax)
		case i: Iterable[P] =>
			if (i.knownSize == 1)
				including(i.head)
			else
				i.minMaxOption match {
					case Some(minMax) => _including(minMax)
					case None => _repr
				}
		case o =>
			o.iterator.minMaxOption match {
				case Some(minMax) => _including(minMax)
				case None => _repr
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
	  * @param point A point
	  * @return A copy of that point that has been restricted to this span.
	  *         The point is adjusted as little as possible.
	  */
	def restrict(point: P) = {
		val _ends = minMax
		if (point < _ends.first)
			_ends.first
		else if (point > _ends.second)
			_ends.second
		else
			point
	}
	
	/**
	  * @param other Another span
	  * @return The overlapping portion between these two spans with the same direction this span has.
	  *         None if these spans don't overlap.
	  */
	def overlapWith(other: Spanning[P, _]) = {
		lazy val otherPoints = {
			if (other.direction == direction)
				other.toPair
			else
				other.toPair.reverse
		}
		Some(start).filter(other.contains).orElse { Some(otherPoints.first).filter(contains) }.map { newStart =>
			val newEnd = Some(end).filter(other.contains).getOrElse(otherPoints.second)
			withEnds(newStart, newEnd)
		}
	}
	/**
	  * Alias for .overlapWith(other)
	  * @param other Another span
	  * @return The overlapping portion between these two spans with the same direction this span has.
	  *         None if these spans don't overlap.
	  */
	def &(other: Spanning[P, _]) = overlapWith(other)
	
	private def _including(minMax: Pair[P]) = {
		val myMinMax = this.minMax
		val newMin = ordering.min(minMax.first, myMinMax.first)
		val newMax = ordering.max(minMax.second, myMinMax.second)
		direction match {
			case Positive => withEnds(newMin, newMax)
			case Negative => withEnds(newMax, newMin)
		}
	}
}
