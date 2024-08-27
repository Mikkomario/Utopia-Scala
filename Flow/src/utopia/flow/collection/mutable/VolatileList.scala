package utopia.flow.collection.mutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair, Single}
import utopia.flow.util.logging.SysErrLogger
import utopia.flow.view.mutable.async.VolatileOld

import scala.collection.immutable.VectorBuilder
import scala.collection.{SeqFactory, mutable}

@deprecated("Deprecated for removal. Please use Volatile.seq or EventfulVolatile.seq instead", "v2.5")
object VolatileList extends SeqFactory[VolatileList]
{
	// IMPLEMENTED  --------------------
	
	override def empty[A] = new VolatileList[A](Empty)
	override def newBuilder[A] = new VectorBuilder[A]().mapResult { apply(_) }
	
	override def from[A](source: IterableOnce[A]) = VolatileList[A](IndexedSeq.from(source))
	
	
	// OTHER    ------------------------
	
    /**
     * Creates a new empty list
     */
    def apply[T]() = empty[T]
    
    /**
     * Creates a new list with a single item
     */
    def apply[T](item: T) = new VolatileList[T](Single(item))
	def apply[T](items: IndexedSeq[T]) = new VolatileList[T](items)
    /**
     * Creates a new list with multiple items
     */
    def apply[T](first: T, second: T, more: T*) = new VolatileList(Pair(first, second) ++ more)
}

/**
* VolatileList is a mutable list that handles items in a thread-safe manner
* @author Mikko Hilpinen
* @since 28.3.2019
**/
@deprecated("Deprecated for removal. Please use Volatile.seq or EventfulVolatile.seq instead", "v2.5")
class VolatileList[T] private(list: IndexedSeq[T])
	extends VolatileOld(list)(SysErrLogger) with mutable.SeqOps[T, VolatileList, VolatileList[T]] with mutable.Seq[T]
{
    // IMPLEMENTED    ---------------
	
	def iterator = value.iterator
	
	def apply(idx: Int) = value(idx)
	
	def length: Int = value.length
	
	override def empty = VolatileList()
	
	override def update(idx: Int, elem: T): Unit = update { _.updated(idx, elem) }
	
	override def seq = this
	override def iterableFactory = VolatileList
	override protected def newSpecificBuilder = VolatileList.newBuilder
	override protected def fromSpecific(coll: IterableOnce[T]) = VolatileList.from(coll)
	
	
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
	def ++=(first: T, second: T, more: T*): Unit = ++=(Pair(first, second) ++ more)
	
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
	def clear() = value = Empty
	
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
	def popAll() = getAndSet(Empty)
}