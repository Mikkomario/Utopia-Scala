package utopia.flow.collection

import utopia.flow.collection.LazyVector.LazyVectorBuilder
import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.datastructure.template.LazyLike

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{IndexedSeqOps, SeqFactory, mutable}
import scala.collection.immutable.VectorBuilder

object LazyVector extends SeqFactory[LazyVector]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * An empty lazily initialized vector
	  */
	private val _empty = apply()
	
	
	// IMPLEMENTED  -----------------------
	
	override def empty[A] = _empty
	
	override def newBuilder[A] = new LazyVectorBuilder[A]()
	
	override def from[A](source: IterableOnce[A]) = source match {
		case l: LazyVector[A] => l
		case s: IndexedSeq[A] => new LazyVector[A](s.map(Lazy.wrap))
		case _ => new LazyVector[A](source.iterator.map(Lazy.wrap).toIndexedSeq)
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Wraps another sequence of items
	  * @param seq A sequence of pre-initialized items
	  * @tparam A Type of stored items
	  * @return Those items wrapped in a lazy vector
	  */
	def wrap[A](seq: IndexedSeq[A]) = new LazyVector[A](seq.map(Lazy.wrap))
	
	/**
	  * Wraps a sequence of lazily initialized items
	  * @param items Items to wrap
	  * @tparam A Type of underlying items
	  * @return A new lazy vector
	  */
	def from[A](items: IndexedSeq[LazyLike[A]]) = new LazyVector[A](items)
	
	/**
	  * Creates a new lazy vector from lazy items
	  * @param items Lazy items
	  * @tparam A Type of wrapped items
	  * @return A new lazy vector
	  */
	def from[A](items: LazyLike[A]*): LazyVector[A] = from(items.toIndexedSeq)
	
	/**
	  * Creates a new lazily initialized vector
	  * @param items Items to place on this vector (as functions that yield a value)
	  * @tparam A Type of vector contents
	  * @return A new lazily initialized vector containing those lazily initialized items
	  */
	def apply[A](items: (() => A)*) = new LazyVector[A](items.toVector.map { i => Lazy(i()) })
	
	
	// NESTED   -------------------------------
	
	/**
	  * A builder for lazy vectors
	  * @tparam A Type of resulting items
	  */
	//noinspection ScalaUnusedExpression
	class LazyVectorBuilder[A] extends mutable.Builder[A, LazyVector[A]]
	{
		// ATTRIBUTES   ----------------------
		
		private val builder = new VectorBuilder[LazyLike[A]]()
		
		
		// IMPLEMENTED  ----------------------
		
		override def knownSize = builder.knownSize
		
		override def clear() = builder.clear()
		
		override def result(): LazyVector[A] = LazyVector.from(builder.result())
		
		override def addOne(elem: A) = {
			builder.addOne(Lazy.wrap(elem))
			this
		}
		
		override def sizeHint(size: Int) = builder.sizeHint(size)
		
		override def addAll(xs: IterableOnce[A]) = {
			builder.addAll(xs.iterator.map(Lazy.wrap))
			this
		}
		
		
		// OTHER    ---------------------------
		
		/**
		  * Adds an item to this vector (which resolves lazily)
		  * @param item An item to add (call-by-name)
		  * @return This builder
		  */
		def +=(item: => A) = {
			builder += Lazy(item)
			this
		}
		/**
		  * Adds a lazily initialized item to this vector
		  * @param item A lazy item to add
		  * @return This builder
		  */
		def +=(item: LazyLike[A]) = {
			builder += item
			this
		}
	}
}

/**
  * A lazily initialized vector class
  * @author Mikko Hilpinen
  * @since 22.7.2022, v1.16
  */
class LazyVector[+A] private(wrapped: IndexedSeq[LazyLike[A]]) extends IndexedSeqOps[A, LazyVector, LazyVector[A]] with IndexedSeq[A]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return The currently initialized contents of this collection
	  */
	def current = wrapped.flatMap { _.current }
	
	
	// IMPLEMENTED  --------------------------
	
	override def length = wrapped.length
	
	override def iterator = wrapped.iterator.map { _.value }
	
	override def iterableFactory = LazyVector
	
	override def empty = LazyVector.empty
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, LazyVector[A]] =
		new LazyVectorBuilder[A]()
	
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
}