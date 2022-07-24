package utopia.flow.collection

import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.datastructure.template.LazyLike

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{IterableFactory, IterableOps, mutable}

object LazyIterable extends IterableFactory[LazyIterable] with LazyFactory[LazyIterable]
{
	// ATTRIBUTES   --------------------------
	
	private val _empty = new LazyIterable(CachingIterable.empty)
	
	
	// IMPLEMENTED  --------------------------
	
	override def empty[A]: LazyIterable[A] = _empty
	
	override def newBuilder[A] =
		LazyBuilder { v: Vector[LazyLike[A]] => new LazyIterable[A](CachingIterable.from(v)) }.precalculated
	
	override def from[A](source: IterableOnce[A]) = source match {
		case l: LazyIterable[A] => l
		case c: CachingIterable[A] => new LazyIterable[A](c.map(Lazy.wrap))
		case c => new LazyIterable[A](CachingIterable(c.iterator.map(Lazy.wrap)))
	}
	
	
	// OTHER    ------------------------------
	
	def apply[A]() = empty[A]
	def apply[A](items: IterableOnce[LazyLike[A]]) = new LazyIterable[A](CachingIterable(items))
}

/**
  * A lazily initialized (but caching) collection
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class LazyIterable[+A] private(wrapped: CachingIterable[LazyLike[A]])
	extends Iterable[A] with IterableOps[A, LazyIterable, LazyIterable[A]] with LazySeqLike[A, LazyIterable]
{
	// COMPUTED -----------------------------
	
	/**
	  * @return A lazily initialized vector containing all elements from this iterable collection
	  */
	def toLazyVector = LazyVector(wrapped.toVector)
	
	
	// IMPLEMENTED  -------------------------
	
	override def iterator = wrapped.iterator.map { _.value }
	
	override protected def factory = LazyIterable
	override protected def lazyContents = wrapped
	
	override def empty = LazyIterable.empty
	override def iterableFactory = LazyIterable
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, LazyIterable[A]] =
		LazyIterable.newBuilder
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		LazyIterable.from(coll)
	
	override def isEmpty = wrapped.isEmpty
	override def size = wrapped.size
	
	// Doesn't record the intermediate state unless necessary
	override def head = wrapped.head.value
	override def headOption = wrapped.headOption.map { _.value }
	// When querying for the last item, uses the vector state
	override def last = wrapped.last.value
	override def lastOption = wrapped.lastOption.map { _.value }
	
	override def tail = new LazyIterable(wrapped.tail)
	override def init = new LazyIterable(wrapped.init)
	
	override def knownSize = wrapped.knownSize
	
	override def toSeq = toLazyVector
	override def toIndexedSeq = toLazyVector
	
	override def sizeCompare(otherSize: Int) = wrapped.sizeCompare(otherSize)
	override def sizeCompare(that: Iterable[_]) = wrapped.sizeCompare(that)
	
	override def map[B](f: A => B) = LazyIterable(wrapped.iterator.map { a => Lazy { f(a.value) } })
	override def flatMap[B](f: A => IterableOnce[B]) =
		LazyIterable(iterator.flatMap { a => f(a).iterator.map(Lazy.wrap) })
	
	override def take(n: Int) = new LazyIterable(wrapped.take(n))
	override def takeRight(n: Int) = new LazyIterable(wrapped.takeRight(n))
	override def takeWhile(p: A => Boolean) = new LazyIterable(wrapped.takeWhile { l => p(l.value) })
	
	override def drop(n: Int) = new LazyIterable(wrapped.drop(n))
	override def dropRight(n: Int) = new LazyIterable(wrapped.dropRight(n))
	override def dropWhile(p: A => Boolean) = new LazyIterable(wrapped.dropWhile { l => p(l.value) })
	
	
	// OTHER    -----------------------------
	
	def ++[B >: A](items: IterableOnce[LazyLike[B]]) = LazyIterable(wrapped ++ items)
	
	/**
	  * Maps the items in this iterable with two levels of caching: 1) The number of items and 2) the content of items
	  * @param f A function that takes an item and returns a number of lazily initialized items
	  * @tparam B Type of the lazily initialized items
	  * @return Lazy map results
	  */
	def lazyFlatMap[B](f: A => IterableOnce[LazyLike[B]]) = LazyIterable(iterator.flatMap { a => f(a) })
}
