package utopia.flow.operator.ordering

import utopia.flow.operator.Identity

import scala.annotation.unused
import scala.language.implicitConversions

object OrderBySequence
{
	// IMPLICIT --------------------------
	
	implicit def objectToInstance[A](@unused o: OrderBySequence.type)
	                                (implicit ord: Ordering[A]): OrderBySequence[A, A] =
		mapping[A, A](Identity)
	
	
	// OTHER    --------------------------
	
	/**
	 * Creates an ordering that maps the sequence values before comparing them
	 * @param f The applied mapping function
	 * @param ord Implicit ordering for the mapping results
	 * @tparam A Type of compared sequences
	 * @tparam B Type of mapping results
	 * @return An ordering
	 */
	def mapping[A, B](f: A => B)(implicit ord: Ordering[B]) =  new OrderBySequence[A, B](f)
}

/**
 * An ordering that compares sequences of items
 * @author Mikko Hilpinen
 * @since 14.03.2025, v2.6
 */
class OrderBySequence[A, B](f: A => B)(implicit ord: Ordering[B]) extends Ordering[Seq[A]]
{
	override def compare(x: Seq[A], y: Seq[A]): Int = {
		val xIter = x.iterator
		val yIter = y.iterator
		var lastResult = 0
		
		while (lastResult == 0 && xIter.hasNext && yIter.hasNext) {
			lastResult = ord.compare(f(xIter.next()), f(yIter.next()))
		}
		
		if (lastResult == 0) {
			if (xIter.hasNext)
				1
			else if (yIter.hasNext)
				-1
			else
				0
		}
		else
			lastResult
	}
}
