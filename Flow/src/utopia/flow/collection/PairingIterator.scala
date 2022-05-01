package utopia.flow.collection

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.datastructure.mutable.MutableLazy
import utopia.flow.util.CollectionExtensions._

/**
  * An iterator wrapper that returns items in pairs
  * @author Mikko Hilpinen
  * @since 1.5.2022, v1.15
  */
class PairingIterator[A](start: => A, source: Iterator[A]) extends Iterator[Pair[A]]
{
	// ATTRIBUTES   ---------------------------
	
	private val nextFirstPointer = new MutableLazy[A](start)
	
	
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
	def +:(item: => A) = new PairingIterator[A](item, start +: source)
	/**
	  * @param item An item to append (call-by-name)
	  * @return An appended version of this iterator
	  */
	def :+(item: => A) = new PairingIterator[A](nextFirst, source :+ item)
}
