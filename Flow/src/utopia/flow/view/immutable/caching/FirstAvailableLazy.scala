package utopia.flow.view.immutable.caching

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.view.mutable.Pointer

import scala.annotation.unchecked.uncheckedVariance

object FirstAvailableLazy
{
	def apply[A](first: Lazy[A], second: Lazy[A], more: Lazy[A]*) =
		new FirstAvailableLazy[A](Pair(first, second) ++ more)
}

/**
 * A lazily initialized container selecting from other lazy values
 * @author Mikko Hilpinen
 * @since 29.07.2025, Fuel v12.1.1
 */
class FirstAvailableLazy[+A](options: Seq[Lazy[A]]) extends Lazy[A]
{
	// ATTRIBUTES   ----------------------
	
	// Stores the value once it has been acquired
	private val pointer: Pointer[Option[A @uncheckedVariance]] = Pointer.empty
	
	
	// IMPLEMENTED  ----------------------
	
	override def current: Option[A] = pointer.value.orElse { options.findMap { _.current } }
	
	override def value: A = pointer.setOneIfEmpty { options.findMap { _.current }.getOrElse { options.head.value } }
}
