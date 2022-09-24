package utopia.flow.operator

import utopia.flow.collection.CollectionExtensions._

object CombinedOrdering
{
	/**
	  * Creates a new combined ordering
	  * @param firstOrdering Primary ordering
	  * @param secondOrdering Secondary ordering
	  * @param moreOrderings Additional orderings
	  * @tparam A Type of ordered item
	  * @return An ordering that combines the specified orderings, using additional orderings to handle cases
	  *         where former orderings return an identical value
	  */
	def apply[A](firstOrdering: Ordering[_ >: A], secondOrdering: Ordering[_ >: A], moreOrderings: Ordering[_ >: A]*) =
		new CombinedOrdering[A](Vector(firstOrdering, secondOrdering) ++ moreOrderings)
}

/**
 * This ordering uses multiple other orderings. If one ordering returns 0, the next one will be used instead.
 * @author Mikko Hilpinen
 * @since 12.1.2020, v1.6.1
 * @param orderings The orderings this ordering delegates ordering to. The first ordering to return a non-zero result
 *                  will be used.
 */
class CombinedOrdering[A](orderings: Seq[Ordering[_ >: A]]) extends Ordering[A]
{
	override def compare(x: A, y: A) = orderings.findMap { order =>
		val result = order.compare(x, y)
		if (result != 0)
			Some(result)
		else
			None
	}.getOrElse(0)
}
