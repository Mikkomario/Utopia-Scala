package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Identity
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

import scala.math.Ordered.orderingToOrdered

object OpenRange
{
	// COMPUTED   -------------------------
	
	/**
	 * @param ord Implicit ordering used
	 * @tparam P Type of this range's theoretical end points
	 * @return An infinite ascending range, spanning all values
	 */
	def infinite[P](implicit ord: Ordering[P]) = apply(None, None)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param start The starting point
	 * @param direction Direction to which this range opens (default = Positive)
	 * @param ord Implicit ordering used
	 * @tparam P Type of the specified start point
	 * @return An open range with the specified starting point
	 */
	def from[P](start: P, direction: Sign = Positive)(implicit ord: Ordering[P]): OpenRange[P] =
		_OpenRange(Some(start), None, ord, isAscending = direction.isPositive)
	/**
	 * @param end The ending point (inclusive)
	 * @param fromDirection Open direction from which the end value is approached (default = Negative)
	 * @param ord Implicit ordering used
	 * @tparam P Type of the specified end point
	 * @return An open range with the specified ending point
	 */
	def to[P](end: P, fromDirection: Sign = Negative)(implicit ord: Ordering[P]): OpenRange[P] =
		_OpenRange(None, Some(end), ord, isAscending = fromDirection.isNegative)
	
	/**
	 * @param start The starting point, if applicable
	 * @param end The ending point, if applicable
	 * @param ascending Whether this range is ascending (true, default) or descending (false).
	 *                  Call-by-name, not called if both 'start' and 'end' have been specified.
	 * @param ord Implicit ordering used
	 * @tparam P Type of the specified end points
	 * @return An open range with the specified end points
	 */
	def apply[P](start: Option[P], end: Option[P], ascending: => Boolean = true)
	            (implicit ord: Ordering[P]): OpenRange[P] =
		Pair(start, end).findForBoth(Identity) match {
			case Some(ends) => Span(ends)
			case None => _OpenRange(start, end, ord, ascending)
		}
	
	
	// NESTED   -----------------------------
	
	private case class _OpenRange[P](startOption: Option[P], endOption: Option[P],
	                                 implicit override val ordering: Ordering[P], override val isAscending: Boolean)
		extends OpenRange[P]
	{
		override protected def self: OpenRange[P] = this
		
		override def reverse: OpenRange[P] = withPossibleEnds(endOption, startOption)
		
		override def withStart(start: P): OpenRange[P] = endOption match {
			case Some(end) => Span(start, end)
			case None => copy(startOption = Some(start))
		}
		override def withEnd(end: P): OpenRange[P] = startOption match {
			case Some(start) => Span(start, end)
			case None => copy(endOption = Some(end))
		}
		override def withEnds(start: P, end: P): OpenRange[P] = Span(start, end)
		override def withPossibleEnds(start: Option[P], end: Option[P]): OpenRange[P] =
			copy(startOption = start, endOption = end)
		
		override def withMin(min: P): OpenRange[P] = {
			if (isAscending) {
				if (endOption.exists { _ < min })
					Span.singleValue(min)
				else
					withStart(min)
			}
			else if (startOption.exists { _ < min })
				Span.singleValue(min)
			else
				withEnd(min)
		}
		override def withMax(max: P): OpenRange[P] = {
			if (isAscending) {
				if (startOption.exists { _ > max })
					Span.singleValue(max)
				else
					withEnd(max)
			}
			else if (endOption.exists { _ > max })
				Span.singleValue(max)
			else
				withStart(max)
		}
		
		override def mapStart(f: P => P): OpenRange[P] = copy(startOption = startOption.map(f))
		override def mapEnd(f: P => P): OpenRange[P] = copy(endOption = endOption.map(f))
		override def mapEnds(f: P => P): OpenRange[P] =
			copy(startOption = startOption.map(f), endOption = endOption.map(f))
		
		override def including(point: P): OpenRange[P] = {
			if (isAscending) {
				if (startOption.exists { _ > point })
					withStart(point)
				else if (endOption.exists { _ < point })
					withEnd(point)
				else
					self
			}
			else if (startOption.exists { _ < point })
				withStart(point)
			else if (endOption.exists { _ > point })
				withEnd(point)
			else
				self
		}
		
		override def isDescending: Boolean = !isAscending
	}
}

/**
 * A range or a span where one or both of the ends may be undefined / open, extending to infinity
 * @author Mikko Hilpinen
 * @since 15.08.2025, v2.7
 */
trait OpenRange[P] extends OpenRangeLike[P, OpenRange[P], OpenRange[P]]