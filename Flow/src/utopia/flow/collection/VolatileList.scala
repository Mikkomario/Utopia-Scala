package utopia.flow.collection

import utopia.flow.async.Volatile
import utopia.flow.util.CollectionExtensions._

import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.VectorBuilder
import scala.collection.mutable

object VolatileList
{
    /**
     * Creates a new empty list
     */
    def apply[T]() = new VolatileList[T](Vector())
    
    /**
     * Creates a new list with a single item
     */
    def apply[T](item: T) = new VolatileList[T](Vector(item))
    
	def apply[T](items: TraversableOnce[T]) = new VolatileList(items.toVector)
	
    /**
     * Creates a new list with multiple items
     */
    def apply[T](first: T, second: T, more: T*) = new VolatileList(Vector(first, second) ++ more)
	
	/**
	  * @tparam A The target type
	  * @return A can build from for Volatile lists of target type
	  */
	def canBuildFrom[A]: CanBuildFrom[VolatileList[_], A, VolatileList[A]] = new VolatileListCanBuildFrom[A]()
}

/**
* VolatileList is a mutable list that handles items in a thread-safe manner
* @author Mikko Hilpinen
* @since 28.3.2019
**/
class VolatileList[T] private(list: Vector[T]) extends Volatile(list) with mutable.SeqLike[T, VolatileList[T]] with mutable.Seq[T]
{
    // IMPLEMENTED    ---------------
    
	def iterator = get.iterator
	
	def apply(idx: Int) = get(idx)
	
	def length: Int = get.length
	
	override def update(idx: Int, elem: T): Unit = update { _.updated(idx, elem) }
	
	override protected def newBuilder = new VolatileListBuilder[T]()
	
	override def seq = this
	
	override def transform(f: T => T) =
	{
		update { _.map(f) }
		this
	}
	
	
	// OPERATORS    ----------------
	
	/**
	 * Adds a new item to the end of this list
	 */
	def :+=(item: T) = update { _ :+ item }
	
	/**
	 * Adds a new item to the beginning of this list
	 */
	def +:=(item: T) = update { _.+:(item) }
	
	/**
	 * Adds multiple new items to this list
	 */
	def ++=(items: TraversableOnce[T]) = update { _ ++ items }
	
	/**
	 * Adds multiple new items to this list
	 */
	def ++=(first: T, second: T, more: T*): Unit = ++=(Vector(first, second) ++ more)
	
	/**
	 * Removes an item from this list
	 */
	def -=(item: Any) = update { _ filterNot { _ == item } }
	
	/**
	 * Removes multiple items from this list
	 */
	def --=(items: Traversable[Any]) = update { _ filterNot { my => items.exists(_ == my) } }
	
	
	// OTHER    --------------------
	
	/**
	 * Clears all items from this list
	 */
	def clear() = set(Vector())
	
	/**
	 * Removes and returns the first item in this list
	 */
	def pop(): Option[T] = pop { v => v.headOption -> v.drop(1) }
	
	/**
	 * Removes and returns the first item in this list that satisfies the provided predicate
	 */
	def popFirst(find: T => Boolean) = pop 
	{
	    items => items.indexWhereOption(find).map 
	    {
	        index => Some(items(index)) -> (items.take(index) ++ items.drop(index + 1))
	    
	    }.getOrElse(None -> items)
	}
}

class VolatileListBuilder[A] extends mutable.Builder[A, VolatileList[A]]
{
	// ATTRIBUTES    ---------------------
	
	private val builder = new VectorBuilder[A]()
	
	
	// IMPLEMENTED    --------------------
	
	override def +=(elem: A): VolatileListBuilder.this.type =
	{
		builder += elem
		this
	}
	
	override def clear() { builder.clear() }
	
	override def result() = VolatileList(builder.result())
}

class VolatileListCanBuildFrom[A] extends CanBuildFrom[VolatileList[_], A, VolatileList[A]]
{
	override def apply(from: VolatileList[_]) = apply()
	
	override def apply() = new VolatileListBuilder[A]()
}