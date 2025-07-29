package utopia.flow.view.immutable.caching

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair

object PrioritizingLazy
{
	def apply[A](first: Lazy[A], second: Lazy[A], more: Lazy[A]*) =
		new PrioritizingLazy[A](Pair(first, second) ++ more)
}

/**
 * A lazily initialized container that utilizes other lazy containers, prioritizing between their available values.
 * @author Mikko Hilpinen
 * @since 29.07.2025, Fuel v12.1.1
 */
class PrioritizingLazy[+A](options: Seq[Lazy[A]]) extends Lazy[A]
{
	// IMPLEMENTED  -------------------
	
	override def current: Option[A] = options.findMap { _.current }
	override def value: A = current.getOrElse { options.head.value }
}
