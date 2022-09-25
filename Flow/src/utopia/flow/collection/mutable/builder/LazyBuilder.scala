package utopia.flow.collection.mutable.builder

import utopia.flow.collection.immutable.caching.iterable.LazyVector
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

object LazyBuilder
{
	/**
	  * Creates a new lazy builder
	  * @param f A function for mapping build results
	  * @tparam A Type of accepted items (within lazy wrappers)
	  * @tparam To Type of resulting collection type
	  * @return A new builder
	  */
	def apply[A, To](f: Vector[Lazy[A]] => To) = new LazyBuilder[A, To](f)
	
	/**
	  * Creates a new lazy vector builder
	  * @tparam A Type of accepted items (within lazy wrappers)
	  * @return A new lazy vector builder
	  */
	def apply[A](): LazyBuilder[A, LazyVector[A]] = apply[A, LazyVector[A]] { LazyVector(_) }
}

/**
  * A builder that accepts lazily initialized items
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
class LazyBuilder[A, +To](f: Vector[Lazy[A]] => To) extends mutable.Builder[Lazy[A], To]
{
	// ATTRIBUTES   ------------------------
	
	private val wrapped = new VectorBuilder[Lazy[A]]()
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return A wrapper around this builder that accepts precalculated (i.e. non-lazy) items
	  */
	def precalculated = Precalculated
	
	
	// IMPLEMENTED  ------------------------
	
	override def clear() = wrapped.clear()
	override def result() = f(wrapped.result())
	
	override def addOne(elem: Lazy[A]) = {
		wrapped.addOne(elem)
		this
	}
	
	override def sizeHint(size: Int) = wrapped.sizeHint(size)
	override def addAll(xs: IterableOnce[Lazy[A]]) = {
		wrapped.addAll(xs)
		this
	}
	override def knownSize = wrapped.knownSize
	
	
	// OTHER    ----------------------------
	
	def addOne(elem: => A): LazyBuilder.this.type = addOne(Lazy(elem))
	def +=(elem: => A): LazyBuilder.this.type = addOne(elem)
	
	
	// NESTED   ----------------------------
	
	/**
	  * A wrapper for this builder that accepts items without a lazy wrapper
	  */
	object Precalculated extends mutable.Builder[A, To]
	{
		override def clear() = LazyBuilder.this.clear()
		
		override def result() = LazyBuilder.this.result()
		
		override def addOne(elem: A) = {
			LazyBuilder.this.addOne(elem)
			this
		}
		
		override def sizeHint(size: Int) = super.sizeHint(size)
		
		override def addAll(xs: IterableOnce[A]) = {
			LazyBuilder.this.addAll(xs.iterator.map(Lazy.initialized))
			this
		}
		
		override def knownSize = LazyBuilder.this.knownSize
	}
}
