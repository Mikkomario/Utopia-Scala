package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.mutable.builder.LazyBuilder
import utopia.flow.collection.template.factory.LazyFactory
import utopia.flow.view.immutable.caching.Lazy

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{IndexedSeqOps, SeqFactory, mutable}

object LazyVector extends SeqFactory[LazyVector] with LazyFactory[LazyVector]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An empty lazily initialized vector
	  */
	private val _empty = new LazyVector(Vector.empty)
	
	
	// IMPLEMENTED  -----------------------
	
	override def empty[A]: LazyVector[A] = _empty
	
	override def newBuilder[A] = LazyBuilder[A]().precalculated
	
	override def from[A](source: IterableOnce[A]) = source match {
		case l: LazyVector[A] => l
		case s: LazySeq[A] => new LazyVector[A](s.lazyContents.toIndexedSeq)
		case s: IndexedSeq[A] => new LazyVector[A](s.map(Lazy.initialized))
		case _ => new LazyVector[A](source.iterator.map(Lazy.initialized).toIndexedSeq)
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Wraps a sequence of lazily initialized items
	  * @param items Items to wrap
	  * @tparam A Type of underlying items
	  * @return A new lazy vector
	  */
	def apply[A](items: IndexedSeq[Lazy[A]]) = new LazyVector[A](items)
	/**
	  * @tparam A Type of vector contents
	  * @return Creates a new empty lazy vector
	  */
	def apply[A]() = empty[A]
	/**
	  * Creates a new lazy vector from lazy items
	  * @tparam A Type of wrapped items
	  * @return A new lazy vector
	  */
	def apply[A](items: Lazy[A]*): LazyVector[A] = apply(items.toIndexedSeq)
	
	override def apply[A](items: IterableOnce[Lazy[A]]) = items match {
		case s: IndexedSeq[Lazy[A]] => new LazyVector[A](s)
		case i => new LazyVector[A](i.iterator.toIndexedSeq)
	}
	
	/**
	  * @param item A single item (call-by-name / lazily initialized)
	  * @tparam A Type of that item
	  * @return A lazily initialized vector that contains that item
	  */
	override def single[A](item: => A) = apply(Vector(Lazy(item)))
	
	/**
	  * Creates a new lazily initialized vector
	  * @param items Items to place on this vector (as functions that yield a value)
	  * @tparam A Type of vector contents
	  * @return A new lazily initialized vector containing those lazily initialized items
	  */
	def fromFunctions[A](items: IndexedSeq[() => A]) = new LazyVector[A](items.map { i => Lazy(i()) })
}

/**
  * A lazily initialized vector class
  * @author Mikko Hilpinen
  * @since 22.7.2022, v1.16
  */
class LazyVector[+A] private(wrapped: IndexedSeq[Lazy[A]])
	extends IndexedSeqOps[A, LazyVector, LazyVector[A]] with IndexedSeq[A] with LazySeqLike[A, LazyVector]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return The currently initialized contents of this collection
	  */
	def current = wrapped.flatMap { _.current }
	
	
	// IMPLEMENTED  --------------------------
	
	override def length = wrapped.length
	
	override protected def factory = LazyVector
	override def lazyContents = wrapped
	
	override def iterator = wrapped.iterator.map { _.value }
	
	override def iterableFactory = LazyVector
	override def empty = LazyVector.empty
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, LazyVector[A]] =
		LazyVector.newBuilder
	
	override def reverse = new LazyVector(wrapped.reverse)
	
	override def apply(i: Int) = wrapped(i).value
	
	override def slice(from: Int, until: Int): LazyVector[A] = new LazyVector[A](wrapped.slice(from, until))
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) = LazyVector.from(coll)
	
	/**
	  * Maps the contents of this collection lazily
	  * @param f A mapping function
	  * @tparam B Type of resulting collection contents
	  * @return A lazily mapped copy of this collection
	  */
	override def map[B](f: A => B) = new LazyVector[B](wrapped.map { l => Lazy { f(l.value) } })
	
	override def prepended[B >: A](elem: B) = new LazyVector[B](wrapped.prepended(Lazy.initialized(elem)))
	
	override def take(n: Int) = new LazyVector(wrapped.take(n))
	override def takeRight(n: Int) = new LazyVector(wrapped.takeRight(n))
	override def drop(n: Int) = new LazyVector(wrapped.drop(n))
	override def dropRight(n: Int) = new LazyVector(wrapped.dropRight(n))
	
	override def appended[B >: A](elem: B) = new LazyVector[B](wrapped.appended(Lazy.initialized(elem)))
	override def prependedAll[B >: A](prefix: IterableOnce[B]) =
		new LazyVector[B](wrapped.prependedAll(prefix.iterator.map(Lazy.initialized)))
	override def appendedAll[B >: A](suffix: IterableOnce[B]) =
		new LazyVector[B](wrapped.appendedAll(suffix.iterator.map(Lazy.initialized)))
	override def padTo[B >: A](len: Int, elem: B) = new LazyVector[B](wrapped.padTo(len, Lazy.initialized(elem)))
}