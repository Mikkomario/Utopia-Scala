package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.WeakList.WeakListBuilder

import scala.annotation.unchecked.uncheckedVariance
import scala.annotation.unused
import scala.collection.immutable.VectorBuilder
import scala.collection.{Factory, IterableOps, SpecificIterableFactory, View, mutable}
import scala.language.implicitConversions
import scala.ref.WeakReference

object WeakList
{
	// ATTRIBUTES   -------------------
	
	/**
	  * An empty weak list instance
	  */
	lazy val empty = new WeakList(Empty)
	
	
	// OTHER    -----------------------
	
	/**
	  * Creates a new empty weak list
	  * @tparam A The type of items in the list
	  * @return A new empty list
	  */
    def apply[A <: AnyRef](): WeakList[A] = empty
	/**
	  * Creates a new weak list with a single item
	  * @param item Target item
	  * @tparam A Type of item
	  * @return A weak list with a single item
	  */
    def apply[A <: AnyRef](item: A) = new WeakList(Single(WeakReference(item)))
	/**
	  * Creates a new weak list with multiple items
	  * @param first The first item
	  * @param second The second item
	  * @param more More items
	  * @tparam A Type of items
	  * @return A new weak list with provided items
	  */
    def apply[A <: AnyRef](first: A, second: A, more: A*) =
        new WeakList[A]((Pair(first, second) ++ more).map { WeakReference(_) })
	
	/**
	  * Creates a new weak list with multiple items
	  * @param items The items that will be added
	  * @tparam A The type of items
	  * @return A new weak list with specified items
	  */
	def from[A <: AnyRef](items: IterableOnce[A]) = items match {
		case w: WeakList[A] => w
		case v: View[A] => new WeakList(OptimizedIndexedSeq.from(v.map(WeakReference.apply)))
		case i: Iterable[A] => new WeakList(OptimizedIndexedSeq.from(i.view.map(WeakReference.apply)))
		case i => new WeakList(OptimizedIndexedSeq.from(i.iterator.map { WeakReference(_) }))
	}
    
	
    // IMPLICIT --------------------
    
	implicit def objectToFactory[A <: AnyRef](@unused o: WeakList.type): Factory[A, WeakList[A]] =
		new WeakListFactory[A]
    
	
	// NESTED   -------------------
	
	private class WeakListBuilder[A <: AnyRef] extends mutable.Builder[A, WeakList[A]]
	{
		// ATTRIBUTES    ---------------------
		
		private val builder = new VectorBuilder[WeakReference[A]]()
		
		
		// IMPLEMENTED    --------------------
		
		override def addOne(elem: A) = {
			builder += WeakReference(elem)
			this
		}
		
		override def clear() = builder.clear()
		override def result() = new WeakList(builder.result())
	}
	
	private class WeakListFactory[A <: AnyRef] extends SpecificIterableFactory[A, WeakList[A]]
	{
		override def empty = WeakList.empty
		override def newBuilder = new WeakListBuilder[A]
		
		override def fromSpecific(it: IterableOnce[A]) = WeakList.from(it)
	}
}

/**
* This list only weakly references items, which means that they may disappear once no other object 
* is referencing them. The class is immutable from the outside, but the list contents may vary 
* based on time so this class doens't have value semantics.
* @author Mikko Hilpinen
* @since 31.3.2019
**/
class WeakList[+A <: AnyRef](refs: Iterable[WeakReference[A]])
	extends Iterable[A] with IterableOps[A, Iterable, WeakList[A]]
{
    // COMPUTED    -----------------
	
	/**
	  * @return A Strongly referenced version of this list
	  */
    def strong = refs.flatMap { _.get }
    
    
    // IMPLEMENTED    --------------
	
	override def iterator = refs.iterator.flatMap { _.get }
	
	override def empty = WeakList.empty
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, WeakList[A]] = new WeakListBuilder[A]
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) = WeakList.from(coll)
    
    
    // OTHER    ----------------------
    
    /**
      * @param item new item
      * @tparam B Type of resulting list
      * @return A new list with the specified item added (weakly referenced)
      */
    def :+[B >: A <: AnyRef](item: B) =
	    new WeakList(OptimizedIndexedSeq.concat(refs.view.filter { _.isEnqueued }, Single(WeakReference(item))))
}