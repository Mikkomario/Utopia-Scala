package utopia.flow.collection.mutable.iterator

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.caching.MutableLazy

object PairingIterator
{
	/**
	  * Creates a new pairing iterator by pairing the items in the specified collection.
	  * Please note that the resulting iterator will be empty if the specified collection contains less than 2 items.
	  * @param coll A collection to iterate
	  * @tparam A Type of the items in the specified collection
	  * @return A new pairing iterator based on the elements of the specified collection.
	  *         E.g. If the specified collection contains elements A, B and C,
	  *         the resulting iterator would return AB, and BC.
	  */
	def apply[A](coll: IterableOnce[A]) = {
		val iter = coll.iterator
		if (iter.hasNext) {
			val start = iter.next()
			new PairingIterator[A](start, iter)
		}
		else
			Iterator.empty
	}
	/**
	  * Creates a new pairing iterator by pairing the items in the specified collection.
	  * Please note that the resulting iterator will be empty if the specified collection is empty.
	  * @param start The first element of the first returned Pair
	  * @param more A collection to iterate
	  * @tparam A Type of the items in the specified collection
	  * @return A new pairing iterator based on the elements of the specified collection.
	  *         E.g. If the specified collection contains elements A, B and C, and 'start' is S,
	  *         the resulting iterator would return SA, AB, and BC.
	  */
	def from[A](start: => A, more: IterableOnce[A]) = new PairingIterator[A](start, more.iterator)
	/**
	  * Creates a new pairing iterator by pairing the items in the specified collection.
	  * @param start The first element of the first returned Pair
	  * @param middle  A collection to iterate
	  * @param end The second element of last returned Pair
	  * @tparam A Type of the items in the specified collection
	  * @return A new pairing iterator based on the elements of the specified collection.
	  *         E.g. If the specified collection contains elements A, B and C, 'start' is S and 'end' is E,
	  *         the resulting iterator would return SA, AB, BC and CE.
	  */
	def between[A](start: => A, middle: IterableOnce[A], end: => A) = {
		val iter = middle.iterator
		if (iter.hasNext)
			new PairingIterator[A](start, iter ++ PollableOnce(end))
		else
			PollableOnce(Pair(start, end))
	}
}

/**
  * An iterator wrapper that returns items in pairs
  * @author Mikko Hilpinen
  * @since 1.5.2022, v1.15
  */
class PairingIterator[A](start: => A, source: Iterator[A]) extends Iterator[Pair[A]]
{
	// ATTRIBUTES   ---------------------------
	
	private val nextFirstPointer = MutableLazy[A](start)
	
	
	// COMPUTED -------------------------------
	
	private def nextFirst = nextFirstPointer.value
	private def nextFirst_=(newVal: A) = nextFirstPointer.value = newVal
	
	
	// IMPLEMENTED  ---------------------------
	
	override def hasNext = source.hasNext
	
	override def next() = {
		val first = nextFirst
		val second = source.next()
		nextFirst = second
		Pair(first, second)
	}
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param item An item to prepend (call-by-name)
	  * @return A prepended version of this iterator
	  */
	def +:(item: => A) = new PairingIterator[A](item, nextFirst +: source)
	/**
	  * @param item An item to append (call-by-name)
	  * @return An appended version of this iterator
	  */
	def :+(item: => A) = new PairingIterator[A](nextFirst, source :+ item)
}
