package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.builder.CompoundingBuilder

import scala.annotation.unchecked.uncheckedVariance

/**
  * A common class for caching collection implementations.
  *
  * == Variance Note ==
  * Implementing classes **must** make sure the builder parameter only receives items of type A.
  * As a consequence, they may use covariance in A and @uncheckedVariance in builder.A
  *
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
abstract class AbstractCachingIterable[+A, +B <: CompoundingBuilder[A @uncheckedVariance, _, _, To], +To <: Iterable[A]]
(source: Iterator[A], protected val builder: B)
	extends Iterable[A]
{
	// COMPUTED --------------------------------
	
	/**
	  * @return The currently cached items
	  */
	def current = builder.currentState
	
	/**
	  * @return Whether this collection has reached its final state (assuming constant source iterator)
	  */
	def isFullyCached = !source.hasNext
	
	/**
	  * @return The size of this collection that's available without any processing.
	  *         The actual size of this collection may be larger, but not smaller, than this value.
	  */
	def minSize = builder.minSize
	/**
	  * @return The current amount of cached items in this collection.
	  *         The actual size of this collection may be larger, but not smaller, than this value.
	  *         Calculation of this value may require some item allocation,
	  *         but doesn't take any new items from the source iterator (i.e. doesn't cache any new items).
	  */
	def currentSize = builder.size
	
	
	// IMPLEMENTED  ----------------------------
	
	override def iterator: Iterator[A] = {
		// Case: Fully cached => Uses the faster and simpler Vector iterator
		if (isFullyCached)
			builder.currentState.iterator
		// Case: Not fully cached => Uses an iterator that initializes items when necessary (default)
		else
			new AppendIfNecessaryIterator()
	}
	
	override def isEmpty = builder.isEmpty && source.isEmpty
	
	// Doesn't record the intermediate state unless necessary
	override def head = builder.headOption.getOrElse { AppendingIterator.next() }
	override def headOption = builder.headOption.orElse { AppendingIterator.nextOption() }
	
	override def last = if (isFullyCached) builder.currentState.last else AppendingIterator.last
	override def lastOption =
		if (isFullyCached) builder.currentState.lastOption else AppendingIterator.lastOption
	
	override def size = {
		if (isFullyCached)
			Some(knownSize).filter { _ >= 0 }.getOrElse { builder.currentSize + AppendingIterator.size }
		else
			builder.currentSize + AppendingIterator.size
	}
	override def knownSize = if (isFullyCached) builder.knownSize else -1
	
	override def sizeCompare(otherSize: Int) = {
		// Compares first with the minimum size, which is cheapest to calculate
		val minCompare = minSize - otherSize
		// Case: Result may be determined using the minimum size => Uses that
		if (minCompare > 0)
			minCompare
		else {
			// Next compares with the current builder state (may require builder update)
			val currentCompare = builder.currentSize - otherSize
			// Case: Result may be determined with the current builder state => Uses that
			if (currentCompare > 0)
				currentCompare
			else {
				// Finally, if the previous comparisons were ambiguous,
				// attempts to cache just enough (diff + 1) items to determine the order
				val offset = -currentCompare
				AppendingIterator.take(offset + 1).size - offset
			}
		}
	}
	override def sizeCompare(that: Iterable[_]) = {
		def defaultCompare = {
			val kn1 = that.knownSize
			if (kn1 < 0) {
				val kn2 = knownSize
				if (kn2 < 0)
					super.sizeCompare(that)
				else
					-that.sizeCompare(kn2)
			}
			else
				sizeCompare(kn1)
		}
		that match {
			case caching: AbstractCachingIterable[_, _, _] =>
				if (isFullyCached)
					-caching.sizeCompare(size)
				else
					defaultCompare
			case _ => defaultCompare
		}
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * @return An iterator that returns items that have not yet been cached by this collection,
	  *         except that they become cached the moment they're returned by this iterator.
	  */
	def cacheIterator: Iterator[A] = AppendingIterator
	
	/**
	  * Caches one item that hasn't been cached already, if possible
	  * @return The cached item. None if all items were cached already.
	  */
	def cacheNext() = AppendingIterator.nextOption()
	/**
	  * Caches the next 'n' items that haven't yet been cached
	  * @param n Number of items to cache
	  * @return The items that were cached. At most of length n.
	  */
	def cacheNext(n: Int) = AppendingIterator.collectNext(n)
	
	/**
	  * Caches all items that haven't yet been cached.
	  * @return The items that were just now cached.
	  */
	def cacheRemaining() = AppendingIterator.toVector
	
	
	// NESTED   ---------------------------
	
	// Appends every encountered item, skips builder items
	private object AppendingIterator extends Iterator[A]
	{
		override def hasNext = source.hasNext
		override def next() = {
			val n = source.next()
			builder += n
			n
		}
	}
	
	// Reads builder items, appends to builder if necessary
	private class AppendIfNecessaryIterator extends Iterator[A]
	{
		private val builderSource = builder.iterator
		
		override def hasNext = builderSource.hasNext || source.hasNext
		
		override def next() = {
			// Appends a new item to the builder if would otherwise run out of items
			if (builderSource.isEmpty)
				builder += source.next()
			// Always reads the items through the builder iterator in order to avoid duplicates
			// Assumes that the builder iterator updates upon +=
			builderSource.next()
		}
	}
}
