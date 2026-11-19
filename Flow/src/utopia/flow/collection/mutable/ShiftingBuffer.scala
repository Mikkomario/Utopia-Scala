package utopia.flow.collection.mutable

import utopia.flow.collection.immutable.OptimizedIndexedSeq

import scala.collection.mutable

/**
 * A buffer that, once full, starts dropping the initially added items, replacing them with new items.
 * @param maxLength Maximum number of buffered items at any one time
 * @author Mikko Hilpinen
 * @since 19.11.2026, v2.9
 */
class ShiftingBuffer[A](maxLength: Int) extends mutable.Builder[A, IndexedSeq[A]] with Iterable[A]
{
	// ATTRIBUTES   --------------------
	
	/**
	 * Wrapped buffer
	 */
	private val buffer = mutable.ArrayBuffer[A]()
	/**
	 * Current buffer length
	 */
	private var len = 0
	/**
	 * Current offset, i.e. number of entries added past [[maxLength]]
	 */
	private var offset = 0
	
	
	// COMPUTED ------------------------
	
	private def isFull = len >= maxLength
	
	
	// IMPLEMENTED  --------------------
	
	override def iterator: Iterator[A] = new BufferIterator
	
	override def knownSize: Int = len
	
	override def head: A = buffer.head
	override def headOption: Option[A] = buffer.headOption
	
	override def last: A = buffer(len - 1)
	override def lastOption: Option[A] = if (len == 0) None else Some(last)
	
	override def tail: Iterable[A] = buffer.tail
	override def init: Iterable[A] = buffer.init
	
	override def toSeq: Seq[A] = result()
	override def toIndexedSeq: IndexedSeq[A] = result()
	override def toVector: Vector[A] = buffer.toVector
	override def toSet[B >: A]: Set[B] = buffer.toSet[B]
	
	override def addOne(elem: A): ShiftingBuffer.this.type = {
		// Case: Full => Replaces the oldest entry
		if (isFull) {
			buffer.remove(0)
			buffer += elem
			offset += 1
		}
		// Case: Growing => Appends to the buffer and updates length
		else {
			buffer += elem
			len += 1
		}
		this
	}
	
	/**
	 * @return The current state of this buffer
	 */
	override def result() = OptimizedIndexedSeq.from(buffer)
	
	override def clear() = {
		buffer.clear()
		len = 0
	}
	
	override def take(n: Int): Iterable[A] = buffer.take(n)
	override def takeRight(n: Int): Iterable[A] = buffer.takeRight(n)
	override def drop(n: Int): Iterable[A] = buffer.drop(n)
	override def dropRight(n: Int): Iterable[A] = buffer.dropRight(n)
	override def slice(from: Int, until: Int): Iterable[A] = buffer.slice(from, until)
	
	
	// OTHER    ------------------------
	
	/**
	 * Removes and returns all buffered items
	 * @return The current contents of this buffer
	 */
	def popAll() = {
		val r = result()
		clear()
		r
	}
	
	
	// NESTED   ------------------------
	
	private class BufferIterator extends Iterator[A]
	{
		// ATTRIBUTES   ----------------
		
		private var lastIndex = -1
		private var lastOffset = 0
		
		
		// IMPLEMENTED  ----------------
		
		override def hasNext: Boolean = {
			if (len <= 0)
				false
			else {
				val shift = offset - lastOffset
				val nextIndex = lastIndex + 1 - shift
				nextIndex < len
			}
		}
		
		override def next(): A = {
			val shift = offset - lastOffset
			lastOffset = offset
			
			val nextIndex = (lastIndex + 1 - shift) max 0
			lastIndex = nextIndex
			
			buffer(nextIndex)
		}
	}
}
