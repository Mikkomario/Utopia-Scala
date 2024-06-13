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
	
	private val _empty = new CachingSeq(Iterator.empty, externallyKnownSize = Some(0))
	
	
	// IMPLEMENTED  ------------------------
	
	override def from[A](source: IterableOnce[A]) = source match {
		case c: CachingSeq[A] => c
		case v: Vector[A] => initialized(v)
		case c => new CachingSeq[A](c.iterator, externallyKnownSize = Some(c.knownSize).filter { _ >= 0 })
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
	  * @param preCached The pre-initialized items at the start of this collection (default = empty)
	  * @tparam A Type of items returned by that iterator
	  * @return A caching iterable based on that iterator
	  */
	def apply[A](source: IterableOnce[A], preCached: Vector[A] = Vector()) = {
		if (preCached.isEmpty)
			from(source)
		else
			new CachingSeq[A](source.iterator, preCached,
				Some(source.knownSize).filter { _ >= 0 }.map { _ + preCached.size })
	}
	
	def apply[A](items: View[A]*) = new CachingSeq[A](items.iterator.map { _.value },
		externallyKnownSize = Some(items.size))
	
	/**
	  * Creates a new pre-initialized sequence by wrapping a vector
	  * @param vector A vector to wrap
	  * @tparam A Type of items in that vector
	  * @return A sequence wrapping that vector
	  */
	def initialized[A](vector: Vector[A]) = new CachingSeq[A](Iterator.empty, vector, Some(vector.size))
	
	/**
	  * @param item A single item (lazily initialized / call-by-name)
	  * @tparam A Type of that item
	  * @return A caching iterable that will contain that item only
	  */
	def single[A](item: => A) = new CachingSeq[A](PollableOnce(item), externallyKnownSize = Some(1))
	
	def fromFunctions[A](items: (() => A)*): CachingSeq[A] =
		new CachingSeq[A](items.iterator.map { _() }, externallyKnownSize = Some(items.size))
}

/**
  * An iterable collection that caches the previously received results
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class CachingSeq[+A](source: Iterator[A], preCached: Vector[A] = Vector[A](), externallyKnownSize: Option[Int] = None)
	extends AbstractCachingIterable[A, CompoundingVectorBuilder[A @uncheckedVariance], Vector[A]](
		source, new CompoundingVectorBuilder[A](preCached), externallyKnownSize)
		with Seq[A] with SeqOps[A, CachingSeq, CachingSeq[A]]
{
	// ATTRIBUTES   ----------------------------
	
	override lazy val length = super[AbstractCachingIterable].size
	
	
	// IMPLEMENTED  ----------------------------
	
	override def empty = CachingSeq.empty
	override def iterableFactory = CachingSeq
	
	override def toVector = fullyCached()
	override def toIndexedSeq = toVector
	
	override def isEmpty = super[AbstractCachingIterable].isEmpty
	
	override def lift = getOption
	
	override def apply(i: Int) = builder.getOption(i).getOrElse { cacheIterator.drop(i - builder.size).next() }
	
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
	
	override def take(n: Int) = {
		if (n <= 0)
			empty
		else {
			val kn = knownSize
			if (kn >= 0 && kn <= n)
				this
			else if (n <= minSize || n <= currentSize)
				CachingSeq.initialized(builder.currentState.take(n))
			else {
				val preCached = builder.currentState
				new CachingSeq[A](iterator.slice(preCached.size, n), preCached, if (kn >= 0) Some(n) else None)
			}
		}
	}
	override def takeRight(n: Int) = {
		if (n <= 0)
			empty
		else {
			val kn = knownSize
			if (kn >= 0 && kn <= n)
				this
			else if (isFullyCached)
				CachingSeq.initialized(builder.currentState.takeRight(n))
			else if (kn >= 0)
				drop(kn - n)
			else
				super.takeRight(n)
		}
	}
	
	override def drop(n: Int) = {
		if (n <= 0)
			this
		else {
			val kn = Some(knownSize).filter { _ >= 0 }
			if (kn.exists { _ <= n })
				empty
			else {
				val knownRemainingSize = kn.map { _ - n }
				if (n < minSize || n < currentSize) {
					val preCached = builder.currentState
					new CachingSeq[A](iterator.drop(preCached.size), preCached.drop(n), knownRemainingSize)
				}
				else
					new CachingSeq[A](iterator.drop(n), externallyKnownSize = knownRemainingSize)
			}
		}
	}
	override def dropRight(n: Int) = {
		if (n <= 0)
			this
		else {
			val kn = knownSize
			if (kn >= 0 && kn <= n)
				empty
			else if (isFullyCached)
				CachingSeq.initialized(builder.currentState.dropRight(n))
			else if (kn >= 0)
				take(kn - n)
			else
				super.dropRight(n)
		}
	}
	
	override def padTo[B >: A](len: Int, elem: B) =
		if (sizeCompare(len) >= 0) this else super.padTo(len, elem)
	
	override def prepended[B >: A](elem: B) = {
		if (isFullyCached)
			CachingSeq.initialized(elem +: fullyCached())
		else
			new CachingSeq[B](Iterator.single(elem) ++ iterator,
				externallyKnownSize = externallyKnownSize.map { _ + 1 })
	}
	override def appended[B >: A](elem: B) = {
		if (isFullyCached)
			CachingSeq(Iterator.single(elem), fullyCached())
		else
			new CachingSeq[B](iterator :+ elem, externallyKnownSize = externallyKnownSize.map { _ + 1 })
	}
	override def prependedAll[B >: A](prefix: IterableOnce[B]) = {
		val iter = prefix.iterator
		if (iter.hasNext)
			new CachingSeq[B](iter ++ iterator,
				externallyKnownSize = Some(knownSize).filter { _ >= 0 }
					.flatMap { mySize => Some(prefix.knownSize).filter { _ >= 0 }.map { mySize + _ } })
		else
			this
	}
	override def appendedAll[B >: A](suffix: IterableOnce[B]) = {
		val iter = suffix.iterator
		if (iter.hasNext) {
			val newKnownSize = Some(knownSize).filter { _ >= 0 }
				.flatMap { mySize => Some(suffix.knownSize).filter { _ >= 0 }.map { mySize + _ } }
			
			if (isFullyCached)
				new CachingSeq[B](iter, fullyCached(), externallyKnownSize = newKnownSize)
			else
				new CachingSeq[B](iterator ++ iter, externallyKnownSize = newKnownSize)
		}
		else
			this
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
