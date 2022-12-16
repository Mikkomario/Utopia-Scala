package utopia.flow.collection.immutable

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.math.Ordered.orderingToOrdered

/**
  * A common trait for iterable items which have two ends: A start and an end.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points in this range
  * @tparam D Type of steps and distances in this range
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait RangeLike[P, D] extends HasEnds[P] with Iterable[P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Amount of distance travelled with a single step or an iteration along this range
	  */
	def step: D
	
	/**
	  * @param distance A distance
	  * @return Positive if that distance is larger than or equal to zero, Negative otherwise
	  */
	protected def signOf(distance: D): Sign
	/**
	  * @param from A starting point
	  * @param distance Distance being traversed
	  * @return A point 'distance' away from the 'from' point
	  */
	protected def traverse(from: P, distance: D): P
	/**
	  * @param distance A distance to invert
	  * @return The inverse of that distance (E.g. inverse of 1 is -1)
	  */
	protected def invert(distance: D): D
	
	
	// IMPLEMENTED  --------------------
	
	override def iterator = _iterator(start, end, step)
	
	override def isEmpty = super[HasEnds].isEmpty
	override def nonEmpty = super[HasEnds].nonEmpty
	
	
	// OTHER    -------------------------
	
	/**
	  * @param step A step taken with each iteration.
	  *
	  *             This parameter is considered an absolute value.
	  *             The actual applied step will always move this range from the 'start' to the 'end'.
	  *             I.e. if this is an ascending range (end > start), the steps taken will always be positive;
	  *             If this range is descending (end < start), the steps taken will always be negative.
	  *
	  * @return An iterator that traverses this range, starting with 'start' and moving one 'step' each iteration
	  */
	def iteratorBy(step: D) = _iterator(start, end, step)
	
	/**
	  * Creates a range iterator
	  * @param start The first returned item
	  * @param end The ending point
	  * @param step Applied step
	  * @return A new iterator
	  */
	protected def _iterator(start: P, end: P, step: D) = {
		// Case: Start and end overlap
		if (start == end) {
			if (isInclusive)
				Iterator.single(start)
			else
				Iterator.empty
		}
		// Case: Traversal is required
		else {
			// Makes sure the step moves to the right direction
			// Inverts it if it doesn't
			val targetSign = if (start < end) Positive else Negative
			val continueCondition: P => Boolean = targetSign match {
				case Positive => if (isInclusive) _ <= end else _ < end
				case Negative => if (isInclusive) _ >= end else _ > end
			}
			val appliedStep = if (signOf(step) == targetSign) step else invert(step)
			Iterator.iterate(start) { traverse(_, appliedStep) }.takeWhile(continueCondition)
		}
	}
}
