package utopia.flow.collection.mutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.async.Volatile

import scala.collection.immutable.VectorBuilder
import scala.collection.{SeqFactory, mutable}

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
    
	def apply[T](items: IterableOnce[T]) = new VolatileList[T](items.iterator.to(Vector))
	
    /**
     * Creates a new list with multiple items
     */
    def apply[T](first: T, second: T, more: T*) = new VolatileList(Vector(first, second) ++ more)
	
	/**
	  * @tparam A The target type
	  * @return A can build from for Volatile lists of target type
	  */
	implicit def factory[A]: VolatileListFactory = new VolatileListFactory
}

/**
* VolatileList is a mutable list that handles items in a thread-safe manner
* @author Mikko Hilpinen
* @since 28.3.2019
**/
class VolatileList[T] private(list: Vector[T]) extends Volatile(list)
	with mutable.SeqOps[T, VolatileList, VolatileList[T]] with mutable.Seq[T]
{
    // IMPLEMENTED    ---------------
	
	def iterator = value.iterator
	
	def apply(idx: Int) = value(idx)
	
	def length: Int = value.length
	
	override def update(idx: Int, elem: T): Unit = update { _.updated(idx, elem) }
	
	override def empty = VolatileList()
	
	override protected def fromSpecific(coll: IterableOnce[T]) = VolatileList(coll)
	
	override protected def newSpecificBuilder = new VolatileListBuilder[T]
	
	override def iterableFactory = VolatileList.factory[T]
	
	override def seq = this
	
	
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
	def ++=(items: IterableOnce[T]) = update { _ ++ items }
	
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
	def --=(items: Iterable[Any]) = update { _ filterNot { my => items.exists(_ == my) } }
	
	
	// OTHER    --------------------
	
	/**
	 * Clears all items from this list
	 */
	def clear() = value = Vector()
	
	/**
	 * Removes and returns the first item in this list
	 */
	def pop(): Option[T] = mutate { v => v.headOption -> v.drop(1) }
	
	/**
	 * Removes and returns the first item in this list that satisfies the provided predicate
	 */
	def popFirst(find: T => Boolean) = mutate { items =>
		items.findLastIndexWhere(find) match {
			case Some(index) => Some(items(index)) -> (items.take(index) ++ items.drop(index + 1))
			case None => None -> items
		}
	}
	
	/**
	  * Clears this list of all items
	  * @return All items that were removed from this list
	  */
	def popAll() = getAndSet(Vector())
}

class VolatileListBuilder[A] extends mutable.Builder[A, VolatileList[A]]
{
	// ATTRIBUTES    ---------------------
	
	private val builder = new VectorBuilder[A]()
	
	
	// IMPLEMENTED    --------------------
	
	override def addOne(elem: A) =
	{
		builder += elem
		this
	}
	
	override def clear() = builder.clear()
	
	override def result() = VolatileList(builder.result())
}

class VolatileListFactory extends SeqFactory[VolatileList]
{
	override def from[A](source: IterableOnce[A]) = VolatileList[A](source)
	
	override def empty[A] = VolatileList[A]()
	
	override def newBuilder[A] = new VolatileListBuilder[A]
}