package utopia.flow.collection.immutable.range

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Extreme.{Max, Min}
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.operator.{Combinable, Extreme, Reversible, Sign}

import scala.math.Ordered.orderingToOrdered

object SpanLike
{
	// EXTENSIONS   ----------------------
	
	implicit class CombinableSpan[P <: Combinable[D, P], D, +Repr](val s: SpanLike[P, Repr]) extends AnyVal
	{
		/**
		  * @param distance A distance to move this span
		  * @return A copy of this span where both the start and the end points have been moved the specified distance
		  */
		def shiftedBy(distance: D) = s._shiftedBy(distance) { _ + _ }
		/**
		  * @param length New length to assign to this span
		  * @return A copy of this span with the same starting point and the new length
		  */
		def withLength(length: D) = s._withLength(length) { _ + _ }
		/**
		  * @param maxLength Largest allowed length
		  * @return A copy of this span with length equal to or smaller than the specified maximum length.
		  *         The starting point of this span is preserved.
		  */
		def withMaxLength(maxLength: D) = s._withMaxLength(maxLength) { _ + _ }
		/**
		  * @param minLength Smallest allowed length
		  * @return A copy of this span with length equal to or larger than the specified minimum length.
		  *         The starting point of this span is preserved.
		  */
		def withMinLength(minLength: D) = s._withMinLength(minLength) { _ + _ }
	}
	
	implicit class SubractableSpan[P <: Combinable[P, P] with Reversible[P], +Repr](val s: SpanLike[P, Repr])
		extends AnyVal
	{
		/**
		  * @return The length of this span
		  */
		def length = s.end - s.start
		
		/**
		  * Moves this span so that it either:
		  * a) Lies completely within the specified span, or
		  * b) Covers the specified span entirely
		  * The applied movement is minimized
		  * @param other Another span
		  * @return A copy of this span that fulfills a condition specified above
		  */
		def shiftedInto(other: HasInclusiveEnds[P]) =
			s._shiftedInto(other) { (a: P, b: P) => a + b } { (a: P, b: P) => a - b }(s.ordering)
	}
}

/**
  * A common trait for items which have two comparable ends, like ranges, and which may be copied and modified in
  * an immutable fashion.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait SpanLike[P, +Repr] extends HasInclusiveOrderedEnds[P]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return "This" element
	  */
	protected def self: Repr
	
	/**
	  * Creates a copy of this element
	  * @param start New starting point (default = current start)
	  * @param end New ending point (default = current end)
	  * @return A copy of this element with the specified start and end points
	  */
	def withEnds(start: P = this.start, end: P = this.end): Repr

	
	// COMPUTED ------------------------
	
	/**
	  * @return A reverse of this span, where the start and end are swapped
	  */
	def reverse = withEnds(end, start)
	
	/**
	  * @return A copy of this span where the 'start' is smaller than the 'end'
	  */
	def ascending = if (isAscending) self else reverse
	/**
	  * @return A copy of this span where the 'start' is larger than the 'end'
	  */
	def descending = if (isDescending) self else reverse
	
	
	// OTHER    -----------------------
	
	/**
	  * @param f A mapping function to apply to the end-points of this span
	  * @return A copy of this span with mapped end-points
	  */
	def mapEnds(f: P => P) = withEnds(f(start), f(end))
	/**
	 * Maps the ends of this span to a different data type
	 * @param f A mapping function applied for both ends of this span
	 * @param ord Ordering to use for the mapping results
	 * @tparam P2 Type of mapping results
	 * @return A new span based on the mapping result of this span
	 */
	def mapTo[P2](f: P => P2)(implicit ord: Ordering[P2]) = Span(f(start), f(end))
	@deprecated("Please use .mapTo(...) instead, as this method involves name conflicts", "v2.2")
	def map[P2](f: P => P2)(implicit ord: Ordering[P2]) = mapTo[P2](f)
	
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
			self
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
	  * @param other Another span
	  * @return The overlapping portion between these two spans with the same direction this span has.
	  *         None if these spans don't overlap.
	  */
	def overlapWith(other: HasInclusiveOrderedEnds[P]) = {
		lazy val otherPoints = {
			if (other.direction == direction)
				other.ends
			else
				other.ends.reverse
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
	def &(other: HasInclusiveOrderedEnds[P]) = overlapWith(other)
	
	/**
	  * @param distance A distance to move this span
	  * @param plus A function that combines a point and a distance
	  * @return A copy of this span where both the start and the end points have been moved the specified distance
	  */
	protected def _shiftedBy[D](distance: D)(plus: (P, D) => P) =
		withEnds(plus(start, distance), plus(end, distance))
	/**
	  * @param length New length to assign to this span
	  * @param plus A function that combines a point and a distance
	  * @return A copy of this span with the same starting point and the new length
	  */
	protected def _withLength[D](length: D)(plus: (P, D) => P) = withEnd(plus(start, length))
	/**
	  * @param maxLength Largest allowed length
	  * @param plus A function that combines a point and a distance
	  * @return A copy of this span with length equal to or smaller than the specified maximum length.
	  *         The starting point of this span is preserved.
	  */
	protected def _withMaxLength[D](maxLength: D)(plus: (P, D) => P) = {
		val newEnd = plus(start, maxLength)
		if (newEnd >= end)
			self
		else
			withEnd(newEnd)
	}
	/**
	  * @param minLength Smallest allowed length
	  * @param plus A function that combines a point and a distance
	  * @return A copy of this span with length equal to or larger than the specified minimum length.
	  *         The starting point of this span is preserved.
	  */
	protected def _withMinLength[D](minLength: D)(plus: (P, D) => P) = {
		val newEnd = plus(start, minLength)
		if (newEnd <= end)
			self
		else
			withEnd(newEnd)
	}
	/**
	  * Moves this span so that it either:
	  * a) Lies completely within the specified span, or
	  * b) Covers the specified span entirely
	  * The applied movement is minimized
	  * @param other Another span
	  * @param plus A function that combines a point and a distance
	  * @param minus A function that calculates the differences between two points
	  * @param ord Implicit ordering to use for distances
	  * @return A copy of this span that fulfills a condition specified above
	  */
	protected def _shiftedInto[D](other: HasInclusiveEnds[P])(plus: (P, D) => P)(minus: (P, P) => D)
	                             (implicit ord: Ordering[D]) =
	{
		if (start < other.start) {
			if (end >= other.end)
				self
			else
				_shiftedBy(ord.min(minus(other.start, start), minus(other.end, end)))(plus)
		}
		else if (end > other.end)
			_shiftedBy(ord.max(minus(other.start, start), minus(other.end, end)))(plus)
		else
			self
	}
	
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
