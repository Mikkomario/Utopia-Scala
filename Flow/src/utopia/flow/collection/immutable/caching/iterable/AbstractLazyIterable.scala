package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.view.immutable.caching.Lazy

/**
  * A common class for lazily initialized, caching collection that utilizes Lazy and CachingIterable
  * @author Mikko Hilpinen
  * @since 30.9.2022, v2.0
  * @tparam A Type of iterated items
  */
class AbstractLazyIterable[+A](wrapped: AbstractCachingIterable[Lazy[A], _, _ <: Iterable[Lazy[A]]])
	extends Iterable[A]
{
	// COMPUTED -----------------------------
	
	/**
	  * @return The currently initialized portion of this sequence
	  */
	def current = wrapped.current.flatMap { _.current }
	
	/**
	  * @return A lazily initialized vector containing all elements from this iterable collection
	  */
	def toLazyVector = LazyVector(wrapped.toVector)
	
	/**
	  * @return Whether this sequence has been completely initialized and there is no lazy computation to perform
	  */
	def isFullyCached = wrapped.isFullyCached && wrapped.forall { _.isInitialized }
	
	
	// IMPLEMENTED  -------------------------
	
	override def iterator = wrapped.iterator.map { _.value }
	
	override def isEmpty = wrapped.isEmpty
	override def size = wrapped.size
	
	// Doesn't record the intermediate state unless necessary
	override def head = wrapped.head.value
	override def headOption = wrapped.headOption.map { _.value }
	override def last = wrapped.last.value
	override def lastOption = wrapped.lastOption.map { _.value }
	
	override def knownSize = wrapped.knownSize
	
	override def toIndexedSeq = toLazyVector
	
	override def sizeCompare(otherSize: Int) = wrapped.sizeCompare(otherSize)
	override def sizeCompare(that: Iterable[_]) = wrapped.sizeCompare(that)
}
