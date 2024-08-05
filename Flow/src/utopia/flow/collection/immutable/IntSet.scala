package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.range.NumericSpan.IntSpan
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.caching.Lazy

object IntSet extends FromCollectionFactory[Int, IntSet]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val empty = apply(Empty)
	
	
	// IMPLEMENTED  --------------------------
	
	override def from(items: IterableOnce[Int]): IntSet = items match {
		case s: IntSet => s
		case s: Seq[Int] => if (s.isEmpty) apply(Empty) else fromPreparedIterator(s.sorted.iterator)
		case i =>
			val s = Seq.from(i)
			if (s.isEmpty) apply(Empty) else fromPreparedIterator(s.sorted.iterator)
	}
	
	override def apply(item: Int): IntSet = apply(Single(NumericSpan.singleValue(item)))
	
	
	// OTHER    ---------------------------
	
	private def fromOrdered(orderedInput: IterableOnce[Int]) = {
		val iterator = orderedInput.iterator
		if (iterator.hasNext)
			fromPreparedIterator(iterator)
		else
			apply(Empty)
	}
	
	// Assumes that the input is sorted and non-empty
	private def fromPreparedIterator(sortedInput: Iterator[Int]): IntSet = {
		val builder = OptimizedIndexedSeq.newBuilder[IntSpan]
		
		var currentStart = sortedInput.next()
		var currentEnd = currentStart
		sortedInput.foreach { i =>
			if (i > currentEnd) {
				if (i == currentEnd + 1)
					currentEnd = i
				else {
					builder += NumericSpan(currentStart, currentEnd)
					currentStart = i
					currentEnd = i
				}
			}
		}
		builder += NumericSpan(currentStart, currentEnd)
		
		apply(builder.result())
	}
}

/**
  * A set of integers, where consecutive sequences are stored as ranges.
  * @author Mikko Hilpinen
  * @since 30.07.2024, v2.4.1
  */
case class IntSet private(ranges: Seq[IntSpan]) extends Iterable[Int]
{
	// ATTRIBUTES   ------------------------
	
	private val lazySize = Lazy { ranges.view.map { _.length }.sum }
	
	
	// IMPLEMENTED  ------------------------
	
	override def iterator: Iterator[Int] = ranges.iterator.flatten
	override def iterableFactory = Set
	
	override def empty = IntSet.empty
	
	override def head = ranges.head.start
	override def last = ranges.last.end
	override def tail = {
		if (ranges.head.length > 1)
			IntSet(NumericSpan(head + 1, ranges.head.last) +: ranges.tail)
		else
			IntSet(ranges.tail)
	}
	
	override def isEmpty = ranges.isEmpty
	
	override def knownSize = lazySize.current.getOrElse(-1)
	override def size = lazySize.value
	
	override def toSeq = ranges.flatten
	override def toIndexedSeq = toSeq.toIndexedSeq
	override def toVector = toSeq.toVector
	
	override protected def reversed = Seq.from(ranges.reverseIterator.flatMap { _.reverseIterator })
	
	override def toString() = s"[${ ranges.mkString(", ") }]"
	
	override def min[B >: Int](implicit ord: Ordering[B]) = head
	override def max[B >: Int](implicit ord: Ordering[B]) = last
	
	override def sizeCompare(otherSize: Int) = lazySize.current match {
		case Some(size) => size.compareTo(otherSize)
		case None =>
			var i = 0
			val stopThreshold = otherSize + 1
			val iter = ranges.iterator
			
			while (i < stopThreshold && iter.hasNext) {
				i += iter.next().length
			}
			
			i.compareTo(otherSize)
	}
	
	override def filter(pred: Int => Boolean) = IntSet.fromOrdered(iterator.filter(pred))
	override def filterNot(pred: Int => Boolean) = IntSet.fromOrdered(iterator.filterNot(pred))
	
	override def take(n: Int) = {
		val builder = OptimizedIndexedSeq.newBuilder[IntSpan]
		var remaining = n
		val input = ranges.iterator
		
		while (remaining > 0 && input.hasNext) {
			val nextRange = input.next()
			if (nextRange.length >= remaining) {
				builder += nextRange
				remaining -= nextRange.length
			}
			else {
				builder += nextRange.withLength(remaining)
				remaining = 0
			}
		}
		
		IntSet(builder.result())
	}
	
	override def takeRight(n: Int) = if (n >= size) this else drop(size - n)
	override def takeWhile(p: Int => Boolean) = IntSet.fromOrdered(iterator.takeWhile(p))
	
	override def drop(n: Int) = {
		if (n <= 0)
			this
		else {
			var partial: Option[IntSpan] = None
			var remaining = n
			val input = ranges.iterator
			
			while (remaining > 0 && input.hasNext) {
				val nextRange = input.next()
				if (nextRange.length >= remaining)
					remaining -= nextRange.length
				else {
					partial = Some(nextRange.mapStart { _ + remaining })
					remaining = 0
				}
			}
			
			IntSet(partial.emptyOrSingle ++ input)
		}
	}
	
	override def dropRight(n: Int) = if (n >= size) IntSet.empty else take(size - n)
	override def dropWhile(p: Int => Boolean) = IntSet.fromOrdered(iterator.dropWhile(p))
	
	
	// OTHER    ------------------------
	
	/**
	  * @param i An integer
	  * @return Whether this set contains that integer
	  */
	def contains(i: Int) = {
		ranges
			.findMap { range =>
				if (range.start > i)
					Some(false)
				else if (range.end < i)
					None
				else
					Some(true)
			}
			.getOrElse(false)
	}
	
	/**
	  * @param i An integer to add to this set
	  * @return Copy of this set including the specified integer
	  */
	// Finds the first range past the targeted index / item
	def +(i: Int) = ranges.findIndexWhere { _.start > i } match {
		case Some(nextIndex) =>
			// Case: i appears before any of the ranges
			if (nextIndex == 0) {
				val firstRange = ranges.head
				// Case: The first range can't be extended to include i => Adds i as a separate range
				if (firstRange.start > i + 1)
					IntSet(firstRange.withStart(i) +: ranges.tail)
				// Case: The first range may be extended
				else
					IntSet(NumericSpan.singleValue(i) +: ranges)
			}
			// Case: i appears between the starts of two ranges
			else {
				val prevIndex = nextIndex - 1
				val prevRange = ranges(prevIndex)
				val nextRange = ranges(nextIndex)
				// Case: i appears between the ranges
				if (prevRange.end < i) {
					// Case: The previous range may be extended to include i
					if (prevRange.end == i - 1) {
						// Case: The next range may also be extended to include i => Joins these ranges together
						if (nextRange.start == i + 1)
							IntSet((ranges.take(prevIndex) :+ NumericSpan(prevRange.start, nextRange.end)) ++
								ranges.drop(nextIndex + 1))
						// Case: Extends the previous range
						else
							IntSet((ranges.take(prevIndex) :+ prevRange.withEnd(i)) ++ ranges.drop(nextIndex))
					}
					// Case: The next range may be extended to include i
					else if (nextRange.start == i + 1)
						IntSet((ranges.take(nextIndex) :+ nextRange.withStart(i)) ++ ranges.drop(nextIndex + 1))
					// Case: Neither of the surrounding ranges may be extended => Adds a new range
					else
						IntSet((ranges.take(nextIndex) :+ NumericSpan.singleValue(i)) ++ ranges.drop(nextIndex))
				}
				// Case: i appears within the previous range => No change is needed
				else
					this
			}
		// Case: i appears after the start of all the ranges
		case None =>
			ranges.lastOption match {
				case Some(lastRange) =>
					// Case: i appears after the end of the last range
					if (lastRange.end < i) {
						// Case: Last range may be extended to contain i
						if (lastRange.end == i - 1)
							IntSet(ranges.dropRight(1) :+ lastRange.withEnd(i))
						// Case: Last range may not be extended => Adds i as a separate range
						else
							IntSet(ranges :+ NumericSpan.singleValue(i))
					}
					// Case: The last range already contains i
					else
						this
						
				// Case: Empty set => Returns a new set containing only i
				case None => IntSet(i)
			}
	}
	
	/**
	  * @param i An integer to remove from this set
	  * @return Copy of this set without the specified integer
	  */
	def -(i: Int) = ranges.findIndexWhere { _.contains(i) } match {
		case Some(targetIndex) =>
			val targetRange = ranges(targetIndex)
			val replacingRanges = {
				if (targetRange.start == i)
					Single(targetRange.withStart(i + 1))
				else if (targetRange.end == i)
					Single(targetRange.withEnd(i - 1))
				else
					Pair(targetRange.withEnd(i - 1), targetRange.withStart(i + 1))
			}
			IntSet(ranges.take(targetIndex) ++ replacingRanges ++ ranges.drop(targetIndex + 1))
			
		case None => this
	}
}