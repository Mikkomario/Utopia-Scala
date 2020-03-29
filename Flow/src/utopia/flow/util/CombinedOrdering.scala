package utopia.flow.util

import utopia.flow.util.CollectionExtensions._

/**
 * This ordering uses multiple other orderings. If one ordering returns 0, the next one will be used instead.
 * @author Mikko Hilpinen
 * @since 12.1.2020, v1.6.1
 * @param orderings The orderings this ordering delegates ordering to. The first ordering to return a non-zero result
 *                  will be used.
 */
class CombinedOrdering[A](orderings: Seq[Ordering[A]]) extends Ordering[A]
{
	override def compare(x: A, y: A) = orderings.findMap { order =>
		val result = order.compare(x, y)
		if (result != 0)
			Some(result)
		else
			None
	}.getOrElse(0)
}
