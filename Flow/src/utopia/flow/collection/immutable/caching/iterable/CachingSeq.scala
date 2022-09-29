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
	extends AbstractCachingIterable[A, CompoundingVectorBuilder[A @uncheckedVariance], Vector[A]](
		source, new CompoundingVectorBuilder[A]())
		with Seq[A] with SeqOps[A, CachingSeq, CachingSeq[A]]
{
	// ATTRIBUTES   ----------------------------
	
	override lazy val length = super[AbstractCachingIterable].size
	
	
	// IMPLEMENTED  ----------------------------
	
	override def empty = CachingSeq.empty
	override def iterableFactory = CachingSeq
	
	override def toVector =
		if (source.hasNext) builder.currentState ++ cacheRemaining() else builder.currentState
	override def toIndexedSeq = toVector
	
	override def knownSize = if (isFullyCached) builder.knownSize else -1
	
	override def apply(i: Int) = builder.getOption(i).getOrElse { iterator.drop(i - builder.size + 1).next() }
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		CachingSeq.from(coll)
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, CachingSeq[A]] =
		CachingSeq.newBuilder
	
	override def lengthCompare(otherSize: Int) = super[AbstractCachingIterable].sizeCompare(otherSize)
	override def lengthCompare(that: Iterable[_]) = super[AbstractCachingIterable].sizeCompare(that)
	
	override def reverse = CachingSeq(reverseIterator)
	override protected def reversed = reverse
	override def reverseIterator =
		cacheRemaining().reverseIterator ++ builder.currentState.reverseIterator
	
	// Avoids calculating the total length of this collection, if at all possible
	override def isDefinedAt(idx: Int) = if (idx < 0) false else sizeCompare(idx) > 0
	
	override def padTo[B >: A](len: Int, elem: B) = {
		if (sizeCompare(len) >= 0)
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
			builder.getOption(index).orElse {
				val diff = index - builder.size
				cacheNext(diff + 1).getOption(diff)
			}
	}
}
