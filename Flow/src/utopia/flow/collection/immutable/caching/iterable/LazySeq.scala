package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.mutable.builder.LazyBuilder
import utopia.flow.collection.template.factory.LazyFactory
import utopia.flow.view.immutable.caching.Lazy

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.SeqOps
import scala.collection.{SeqFactory, mutable}

object LazySeq extends SeqFactory[LazySeq] with LazyFactory[LazySeq]
{
	// ATTRIBUTES   --------------------------
	
	private val _empty = new LazySeq(CachingSeq.empty)
	
	
	// IMPLEMENTED  --------------------------
	
	override def empty[A]: LazySeq[A] = _empty
	
	override def newBuilder[A] =
		LazyBuilder { v: Vector[Lazy[A]] => new LazySeq[A](CachingSeq.from(v)) }.precalculated
	
	override def from[A](source: IterableOnce[A]) = source match {
		case l: LazySeq[A] => l
		case v: LazyVector[A] => new LazySeq[A](CachingSeq.from(v.lazyContents))
		case c: CachingSeq[A] => new LazySeq[A](c.map(Lazy.initialized))
		case c => new LazySeq[A](CachingSeq(c.iterator.map(Lazy.initialized)))
	}
	
	
	// OTHER    ------------------------------
	
	def apply[A]() = empty[A]
	def apply[A](items: IterableOnce[Lazy[A]]) = new LazySeq[A](CachingSeq(items))
}

/**
  * A lazily initialized (but caching) collection
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class LazySeq[+A] private(wrapped: CachingSeq[Lazy[A]])
	extends AbstractLazyIterable[A](wrapped)
		with Seq[A] with SeqOps[A, LazySeq, LazySeq[A]] with LazySeqLike[A, LazySeq]
{
	// IMPLEMENTED  -------------------------
	
	override protected def factory = LazySeq
	override def lazyContents = wrapped
	
	override def empty = LazySeq.empty
	override def iterableFactory = LazySeq
	
	override def apply(i: Int) = wrapped(i).value
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, LazySeq[A]] =
		LazySeq.newBuilder
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		LazySeq.from(coll)
	
	override def isEmpty = wrapped.isEmpty
	override def length = wrapped.length
	
	override def tail = new LazySeq(wrapped.tail)
	override def init = new LazySeq(wrapped.init)
	
	override def lengthCompare(otherSize: Int) = wrapped.lengthCompare(otherSize)
	override def lengthCompare(that: Iterable[_]) = wrapped.lengthCompare(that)
	
	override def map[B](f: A => B) = LazySeq(wrapped.iterator.map { a => Lazy { f(a.value) } })
	override def flatMap[B](f: A => IterableOnce[B]) =
		LazySeq(iterator.flatMap { a => f(a).iterator.map(Lazy.initialized) })
	
	override def take(n: Int) = new LazySeq(wrapped.take(n))
	override def takeRight(n: Int) = new LazySeq(wrapped.takeRight(n))
	override def takeWhile(p: A => Boolean) = new LazySeq(wrapped.takeWhile { l => p(l.value) })
	
	override def drop(n: Int) = new LazySeq(wrapped.drop(n))
	override def dropRight(n: Int) = new LazySeq(wrapped.dropRight(n))
	override def dropWhile(p: A => Boolean) = new LazySeq(wrapped.dropWhile { l => p(l.value) })
	
	override def prepended[B >: A](elem: B) = new LazySeq[B](wrapped.prepended(Lazy.initialized(elem)))
	override def appended[B >: A](elem: B) = new LazySeq[B](wrapped.appended(Lazy.initialized(elem)))
	override def prependedAll[B >: A](prefix: IterableOnce[B]) =
		new LazySeq[B](wrapped.prependedAll(prefix.iterator.map(Lazy.initialized)))
	override def appendedAll[B >: A](suffix: IterableOnce[B]) =
		new LazySeq[B](wrapped.appendedAll(suffix.iterator.map(Lazy.initialized)))
	
	override def distinct = new LazySeq(wrapped.distinctBy { _.value })
	override def distinctBy[B](f: A => B) = new LazySeq(wrapped.distinctBy { a => f(a.value) })
	
	override def reverse = new LazySeq(wrapped.reverse)
	override def reverseIterator = wrapped.reverseIterator.map { _.value }
	
	override def isDefinedAt(idx: Int) = wrapped.isDefinedAt(idx)
	
	override def padTo[B >: A](len: Int, elem: B) = new LazySeq[B](wrapped.padTo(len, Lazy.initialized(elem)))
	override def updated[B >: A](index: Int, elem: B) =
		new LazySeq[B](wrapped.updated(index, Lazy.initialized(elem)))
	
	
	// OTHER    -----------------------------
	
	def ++[B >: A](items: IterableOnce[Lazy[B]]) = LazySeq(wrapped ++ items)
	
	/**
	  * Maps the items in this iterable with two levels of caching: 1) The number of items and 2) the content of items
	  * @param f A function that takes an item and returns a number of lazily initialized items
	  * @tparam B Type of the lazily initialized items
	  * @return Lazy map results
	  */
	def lazyFlatMap[B](f: A => IterableOnce[Lazy[B]]) = LazySeq(iterator.flatMap { a => f(a) })
}
