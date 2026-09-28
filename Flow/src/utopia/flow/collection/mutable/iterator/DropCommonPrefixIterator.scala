package utopia.flow.collection.mutable.iterator

import scala.annotation.unchecked.uncheckedVariance

object DropCommonPrefixIterator
{
	/**
	 * @param source Collection from which items are returned
	 * @param other Collection that may contain a common prefix
	 * @tparam A Type of iterated items
	 * @return An iterator that yields 'source' without any common leading elements from 'other'.
	 */
	def apply[A](source: IterableOnce[A], other: IterableOnce[_ >: A]) =
		new DropCommonPrefixIterator(source.iterator, other.iterator)
}

/**
 * An iterator that drops the common prefix with another collection.
 * @author Mikko Hilpinen
 * @since 28.09.2026, v2.9
 */
class DropCommonPrefixIterator[+A](source: Iterator[A], other: Iterator[_ >: A]) extends Iterator[A]
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * Contains the first distinct item in 'source', or None if the end of 'other' was reached first.
	 */
	private lazy val firstDistinct = {
		// Finds the next distinct item or the end of 'other'
		var next: Option[A @uncheckedVariance] = None
		while (next.isEmpty && other.hasNext && source.hasNext) {
			val a = source.next()
			if (a != other.next())
				next = Some(a)
		}
		next
	}
	/**
	 * Contains true after [[firstDistinct]] has been used / returned.
	 */
	private var firstDistinctConsumed = false
	
	
	// IMPLEMENTED  ---------------------
	
	override def hasNext: Boolean = {
		if (firstDistinctConsumed)
			source.hasNext
		else
			firstDistinct.isDefined || (source.hasNext && !other.hasNext)
	}
	
	override def next(): A = {
		if (firstDistinctConsumed)
			source.next()
		else {
			firstDistinctConsumed = true
			firstDistinct.getOrElse { source.next() }
		}
	}
}
