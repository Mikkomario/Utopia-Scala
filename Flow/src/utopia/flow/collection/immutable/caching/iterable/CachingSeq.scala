package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.builder.CompoundingVectorBuilder
import utopia.flow.collection.mutable.iterator.PollableOnce
import utopia.flow.view.immutable.View

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.{SeqOps, VectorBuilder}
import scala.collection.{SeqFactory, mutable}

object CachingSeq extends SeqFactory[CachingSeq]
{
	// ATTRIBUTES   ------------------------
	
	private val _empty = new CachingSeq(Iterator.empty)
	
	
	// IMPLEMENTED  ------------------------
	
	override def from[A](source: IterableOnce[A]) = source match {
		case c: CachingSeq[A] => c
		case c => new CachingSeq[A](c.iterator)
	}
	
	override def empty[A]: CachingSeq[A] = _empty
	
	override def newBuilder[A] =
		new VectorBuilder[A]().mapResult { v => new CachingSeq[A](v.iterator) }
	
	
	// OTHER    ----------------------------
	
	/**
	  * Wraps an iterator into a caching iterable. Please note that this doesn't consume the iterator all at once,
	  * only lazily. Modifications to that iterator from other sources will affect the resulting collection
	  * (and are not recommended).
	  * @param source A source iterator
	  * @tparam A Type of items returned by that iterator
	  * @return A caching iterable based on that iterator
	  */
	def apply[A](source: IterableOnce[A]) = new CachingSeq[A](source.iterator)
	def apply[A](items: View[A]*) = new CachingSeq[A](items.iterator.map { _.value })
	
	/**
	  * @param item A single item (lazily initialized / call-by-name)
	  * @tparam A Type of that item
	  * @return A caching iterable that will contain that item only
	  */
	def single[A](item: => A) = new CachingSeq[A](PollableOnce(item))
	
	def fromFunctions[A](items: (() => A)*): CachingSeq[A] = apply(items.map { _() })
}

/**
  * An iterable collection that caches the previously received results
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class CachingSeq[+A](source: Iterator[A])
	extends Seq[A] with SeqOps[A, CachingSeq, CachingSeq[A]]
{
	// ATTRIBUTES   ----------------------------
	
	// Builder will only be fed items from the source iterator, hence allowing @uncheckedVariance
	private val builder: CompoundingVectorBuilder[A @uncheckedVariance] = new CompoundingVectorBuilder[A]()
	
	override lazy val length = builder.currentSize + AppendingIterator.size
	
	
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
	
	override def iterator: Iterator[A] = new AppendIfNecessaryIterator()
	
	override def empty = CachingSeq.empty
	override def iterableFactory = CachingSeq
	
	override def toVector =
		if (source.hasNext) builder.currentState ++ AppendingIterator else builder.currentState
	override def toIndexedSeq = toVector
	
	override def isEmpty = builder.isEmpty && source.isEmpty
	
	// Doesn't record the intermediate state unless necessary
	override def head = builder.headOption.getOrElse { AppendingIterator.next() }
	override def headOption = builder.headOption.orElse { AppendingIterator.nextOption() }
	
	override def last = if (isFullyCached) builder.currentState.last else AppendingIterator.last
	override def lastOption =
		if (isFullyCached) builder.currentState.lastOption else AppendingIterator.lastOption
	
	override def knownSize = if (isFullyCached) builder.knownSize else -1
	
	override def apply(i: Int) = builder.getOption(i).getOrElse { iterator.drop(i - builder.size + 1).next() }
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		CachingSeq.from(coll)
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, CachingSeq[A]] =
		CachingSeq.newBuilder
	
	override def lengthCompare(otherSize: Int) = {
		val knownSize = builder.knownSize
		val knownSizeCompare = knownSize.compareTo(otherSize)
		// Case: Result can be determined with the size of cached contents => Doesn't iterate forward
		if (knownSizeCompare > 1)
			knownSizeCompare
		// Case: Comparison result unknown with the known size alone
		else {
			val appendSize = AppendingIterator.take(otherSize - knownSize + 1).size
			(knownSize + appendSize).compareTo(otherSize)
		}
	}
	override def lengthCompare(that: Iterable[_]) = {
		val kn = that.knownSize
		if (kn < 0)
			super.sizeCompare(that)
		else
			sizeCompare(kn)
	}
	
	override def reverse = CachingSeq(reverseIterator)
	override protected def reversed = reverse
	override def reverseIterator =
		AppendingIterator.toVector.reverseIterator ++ builder.currentState.reverseIterator
	
	// Avoids calculating the total length of this collection, if at all possible
	override def isDefinedAt(idx: Int) = {
		if (idx < 0)
			false
		else if (idx < builder.minSize)
			true
		else {
			val cachedSize = builder.size
			if (idx < cachedSize)
				true
			else
				AppendingIterator.drop(idx - cachedSize).hasNext
		}
	}
	
	override def padTo[B >: A](len: Int, elem: B) = {
		if (len <= minSize || len <= currentSize)
			this
		else
			super.padTo(len, elem)
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param index Targeted index
	  * @return The item in this collection at that index. None if that index is not valid.
	  */
	def getOption(index: Int) = {
		if (index < 0)
			None
		else
			builder.getOption(index).orElse { AppendingIterator.drop(index - builder.size).nextOption() }
	}
	
	
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
