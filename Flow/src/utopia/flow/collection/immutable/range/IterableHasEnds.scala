package utopia.flow.collection.immutable.range

import utopia.flow.operator.Steppable
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

import scala.language.implicitConversions
import scala.math.Ordered.orderingToOrdered

object IterableHasEnds
{
	// OTHER    -------------------------
	
	/**
	  * @param start Range starting value (inclusive)
	  * @param end Range end value (inclusive or exlusive)
	  * @param exclusive Whether the end value is exclusive (default = false)
	  * @param traverse A function that accepts a start point value, and the direction of travel (binary) and
	  *                 yields the next step.
	  *
	  *                 E.g. an integer-based function could accept (2, Positive) and yield 3.
	  * @param ord Implicit ordering to apply
	  * @tparam P Type of range end-points
	  * @return A new iterable range
	  */
	def iterate[P](start: P, end: P, exclusive: Boolean = false)
	            (traverse: (P, Sign) => P)
	            (implicit ord: Ordering[P]): IterableHasEnds[P] =
		new _IterableHasEnds[P](start, end, !exclusive)(traverse)
	
	/**
	  * @param start     Range starting value (inclusive)
	  * @param end       Range end value (inclusive or exlusive)
	  * @param exclusive Whether the end value is exclusive (default = false)
	  * @param ord       Implicit ordering to apply
	  * @tparam P Type of range end-points
	  * @return A new iterable range
	  */
	def apply[P <: Steppable[P]](start: P, end: P, exclusive: Boolean = false)(implicit ord: Ordering[P]) =
		iterate(start, end, exclusive) { _ next _ }
	
	/**
	  * Wraps another range that has supported value type
	  * @param other Another range
	  * @tparam P Type of range end-points
	  * @return A new iterable copy of that range
	  */
	implicit def wrap[P <: Steppable[P]](other: HasOrderedEnds[P]): IterableHasEnds[P] =
		apply(other.start, other.end, other.isExclusive)(other.ordering)
	
	
	// NESTED   -------------------------
	
	private class _IterableHasEnds[P](override val start: P, override val end: P, override val isInclusive: Boolean)
	                                 (f: (P, Sign) => P)(implicit override val ordering: Ordering[P])
		extends IterableHasEnds[P]
	{
		override protected def traverse(from: P, direction: Sign): P = f(from, direction)
	}
}

/**
  * A common trait for iterable items which have two ends: A start and an end.
  * The end-point may be inclusive or exclusive.
  * @tparam P Type of end-points in this range
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait IterableHasEnds[P] extends HasOrderedEnds[P] with Iterable[P]
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
	
	override def isEmpty = super[HasOrderedEnds].isEmpty
	override def nonEmpty = super[HasOrderedEnds].nonEmpty
	
	
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
