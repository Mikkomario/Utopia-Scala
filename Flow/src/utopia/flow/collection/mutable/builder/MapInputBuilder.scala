package utopia.flow.collection.mutable.builder

import scala.collection.mutable

/**
 * A builder wrapper that transforms the builder input
 * @tparam E Type of input accepted by this builder
 * @tparam I Type of input accepted by the wrapped builder
 * @tparam To Type of collections built
 * @author Mikko Hilpinen
 * @since 25.11.2025, v2.8
 */
class MapInputBuilder[-E, I, +To](wrapped: mutable.Builder[I, To])(f: E => I) extends mutable.Builder[E, To]
{
	override def knownSize = wrapped.knownSize
	
	override def clear() = wrapped.clear()
	override def result() = wrapped.result()
	
	override def addOne(elem: E) = {
		wrapped.addOne(f(elem))
		this
	}
	override def addAll(elems: IterableOnce[E]) = {
		wrapped.addAll(elems.iterator.map(f))
		this
	}
	
	override def sizeHint(size: Int) = wrapped.sizeHint(size)
}
