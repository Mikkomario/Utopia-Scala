package utopia.flow.collection.immutable.range

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.math.Ordered.orderingToOrdered

/**
  * A common trait for iterable items which have two ends: A start and an end.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points in this range
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait IterableHasEnds[P] extends HasEnds[P] with Iterable[P]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param from A starting point
	  * @param direction Distance being travelled to
	  * @return A point next to the 'from' towards the specified direction
	  */
	protected def traverse(from: P, direction: Sign): P
	
	
	// IMPLEMENTED  --------------------
	
	override def iterator = _iterator(start, end)
	
	override def isEmpty = super[HasEnds].isEmpty
	override def nonEmpty = super[HasEnds].nonEmpty
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a range iterator
	  * @param start The first returned item
	  * @param end The ending point
	  * @return A new iterator
	  */
	protected def _iterator(start: P, end: P) = {
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
			Iterator.iterate(start) { traverse(_, targetSign) }.takeWhile(continueCondition)
		}
	}
}
