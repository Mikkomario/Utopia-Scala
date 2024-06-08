package utopia.flow.collection.mutable.builder

import utopia.flow.collection.immutable.Pair
import utopia.flow.view.mutable.caching.ResettableLazy

import scala.collection.mutable

/**
  * A map builder that may be used and iterated through during building
  * @author Mikko Hilpinen
  * @since 29.9.2022, v2.0
  */
class CompoundingMapBuilder[K, V](initialState: Map[K, V] = Map[K, V]())
	extends CompoundingBuilder[(K, V), mutable.Builder[(K, V), mutable.Map[K, V]], mutable.Map[K, V], Map[K, V]](initialState)
		with scala.collection.Map[K, V]
{
	// ATTRIBUTES   --------------------------
	
	private val builderPointer = ResettableLazy { mutable.Map[K, V]() }
	
	
	// COMPUTED ------------------------------
	
	private def currentBuilder = builderPointer.current
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def clearState = Map()
	
	override protected def newBuilder(): mutable.Builder[(K, V), mutable.Map[K, V]] =
		new MutableMapBuilder(builderPointer.newValue())
	
	override protected def append(newItems: mutable.Map[K, V]): Map[K, V] = lastResult ++ newItems
	
	// Always knows size because of the type of builder utilized
	override def size = {
		val builderSize = currentBuilder match {
			case Some(b) => b.size
			case None => 0
		}
		lastResult.size + builderSize
	}
	override def currentSize = size
	override def minSize = size
	override def knownSize = size
	
	override def isEmpty = lastResult.isEmpty && currentBuilder.forall { _.isEmpty }
	
	override def head = if (lastResult.isEmpty) currentBuilder.head.head else lastResult.head
	override def headOption = lastResult.headOption.orElse(currentBuilder.flatMap { _.headOption })
	override def last = currentBuilder.filter { _.nonEmpty } match {
		case Some(b) => b.last
		case None => lastResult.last
	}
	override def lastOption = currentBuilder.flatMap { _.lastOption }.orElse { lastResult.lastOption }
	
	override def toMap[K2, V2](implicit ev: (K, V) <:< (K2, V2)) = currentState.toMap
	
	override def get(key: K) = currentBuilder.flatMap { _.get(key) }.orElse { lastResult.get(key) }
	
	override def -(key: K) = currentState - key
	override def -(key1: K, key2: K, keys: K*) = currentState -- (Pair(key1, key2) ++ keys)
	
	override def contains(key: K) = lastResult.contains(key) || currentBuilder.exists { _.contains(key) }
}

private class MutableMapBuilder[K, V](wrapped: mutable.Map[K, V]) extends mutable.Builder[(K, V), mutable.Map[K, V]]
{
	override def knownSize = wrapped.size
	
	override def result() = wrapped
	
	override def clear() = wrapped.clear()
	
	override def addOne(elem: (K, V)) = {
		wrapped += elem
		this
	}
	override def addAll(xs: IterableOnce[(K, V)]) = {
		wrapped ++= xs
		this
	}
}