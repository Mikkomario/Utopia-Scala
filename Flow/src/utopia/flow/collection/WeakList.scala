package utopia.flow.collection

import scala.collection.immutable.VectorBuilder
import scala.collection.{Factory, IterableFactory, IterableOps, SpecificIterableFactory, mutable}
import scala.ref.WeakReference

object WeakList
{
	/**
	  * Creates a new empty weak list
	  * @tparam A The type of items in the list
	  * @return A new empty list
	  */
    def apply[A <: AnyRef]() = new WeakList[A](Vector())
	
	/**
	  * Creates a new weak list with a single item
	  * @param item Target item
	  * @tparam A Type of item
	  * @return A weak list with a single item
	  */
    def apply[A <: AnyRef](item: A) = new WeakList(Vector(WeakReference(item)))
	
	/**
	  * Creates a new weak list with multiple items
	  * @param items The items that will be added
	  * @tparam A The type of items
	  * @return A new weak list with specified items
	  */
	def apply[A <: AnyRef](items: IterableOnce[A]) = new WeakList(items.iterator.map { WeakReference(_) }.toVector)
	
	/**
	  * Creates a new weak list with multiple items
	  * @param first The first item
	  * @param second The second item
	  * @param more More items
	  * @tparam A Type of items
	  * @return A new weak list with provided items
	  */
    def apply[A <: AnyRef](first: A, second: A, more: A*) =
    {
        val items: Vector[A] = Vector(first, second) ++ more
        new WeakList[A](items.map { WeakReference(_) })
    }
    
	
    // IMPLICIT --------------------
    
    /**
      * Implicit canBuildFrom for this class
      * @tparam A type of item contained in the final list
      * @return A can build from that will build weak lists of target type
      */
    implicit def factory[A <: AnyRef]: Factory[A, WeakList[A]] = new WeakListFactory[A]
}

/**
* This list only weakly references items, which means that they may disappear once no other object 
* is referencing them. The class is immutable from the outside, but the list contents may vary 
* based on time so this class doens't have value semantics.
* @author Mikko Hilpinen
* @since 31.3.2019
**/
class WeakList[A <: AnyRef](private val refs: Vector[WeakReference[A]])
	extends IterableOps[A, Iterable, WeakList[A]] with Iterable[A]
{
    // COMPUTED    -----------------
	
	/**
	  * @return A Strongly referenced version of this list
	  */
    def strong = refs.flatMap { _.get }
    
    
    // IMPLEMENTED    --------------
	
	override def empty = WeakList[A]()
	
	override protected def newSpecificBuilder = new WeakListBuilder[A]
	
	override protected def fromSpecific(coll: IterableOnce[A]) = WeakList(coll)
	
	override def seq = this
    
    override def iterator = refs.view.flatMap { _.get }.iterator
    
    override def foreach[U](f: A => U) = refs.foreach { _.get.foreach(f) }
    
    
    // OPERATORS    ----------------------
    
    /**
      * @param item new item
      * @tparam B Type of resulting list
      * @return A new list with the specified item added (weakly referenced)
      */
    def :+[B >: A <: AnyRef](item: B) = new WeakList(refs :+ WeakReference(item))
    
    /*
      * @param items new items
      * @tparam B Type of resulting list
      * @return A new list with specified items added (weakly referenced)
      */
    // def ++[B >: A <: AnyRef](items: IterableOnce[B]) = new WeakList(refs ++ items.map { WeakReference(_) })
}

class WeakListBuilder[A <: AnyRef] extends mutable.Builder[A, WeakList[A]]
{
    // ATTRIBUTES    ---------------------
    
    private val builder = new VectorBuilder[WeakReference[A]]()
    
    
    // IMPLEMENTED    --------------------
	
	override def addOne(elem: A) =
	{
		builder += WeakReference(elem)
		this
	}
	
	override def clear() = builder.clear()
    
    override def result() = new WeakList(builder.result())
}

class WeakListFactory[A <: AnyRef] extends SpecificIterableFactory[A, WeakList[A]]
{
	override def empty = WeakList[A]()
	
	override def newBuilder = new WeakListBuilder[A]
	
	override def fromSpecific(it: IterableOnce[A]) = WeakList(it)
}