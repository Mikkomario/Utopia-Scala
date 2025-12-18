package utopia.flow.collection.mutable.builder

import scala.collection.mutable

/**
 * A builder which flattens the appended collections
 * @author Mikko Hilpinen
 * @since 17.12.2025, v2.8
 */
class FlatteningBuilder[-A, +To](wrapped: mutable.Builder[A, To]) extends mutable.Builder[IterableOnce[A], To]
{
	// IMPLEMENTED  ---------------------
	
	override def knownSize: Int = wrapped.knownSize
	
	override def clear(): Unit = wrapped.clear()
	override def result(): To = wrapped.result()
	
	override def addOne(elem: IterableOnce[A]): FlatteningBuilder.this.type = {
		wrapped ++= elem
		this
	}
	override def addAll(elems: IterableOnce[IterableOnce[A]]): FlatteningBuilder.this.type = {
		wrapped ++= elems.iterator.flatten
		this
	}
}
