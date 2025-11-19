package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.range.{HasInclusiveEnds, NumericSpan}
import utopia.flow.collection.immutable.range.NumericSpan.IntSpan
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.IntSet.IntSetBuilder
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.{View, mutable}

object IntSet extends FromCollectionFactory[Int, IntSet]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val empty = new IntSet(Empty)
	
	
	// COMPUTED ------------------------------
	
	/**
	 * @return A new IntSet builder
	 */
	def newBuilder = new IntSetBuilder
	
	
	// IMPLEMENTED  --------------------------
	
	override def from(items: IterableOnce[Int]): IntSet = items match {
		// Case: Already an IntSet
		case s: IntSet => s
		// Case: A span => Wraps it
		case s: IntSpan => apply(Single(s.ascending))
		// Case: A span-like element => Converts it to a span and wraps it
		case s: HasInclusiveEnds[Int] => apply(Single(NumericSpan(s.ends.sorted)))
		// Case: An inclusive range => Wraps it as a span
		case r: Range.Inclusive => apply(Single[NumericSpan[Int]](r))
		// Case: An exclusive range => Wraps it as a span, if not empty
		case r: Range => if (r.isEmpty) empty else apply(Single(NumericSpan(r.start, r.end - 1)))
		// Case An ordered sequence
		case s: Seq[Int] =>
			// Case: Empty => Skips building
			if (s.isEmpty)
				empty
			// Case: Ordered => Builds using a simpler function
			else if (s.iterator.paired.forall { p => p.first <= p.second })
				fromPreparedIterator(s.iterator)
			// Case: Unordered => Uses a builder
			else {
				val builder = newBuilder
				builder ++= s
				builder.result()
			}
		
		// Case: Other type of collection => Uses a builder
		case i =>
			i.nonEmptyIterator match {
				case Some(iterator) =>
					val builder = newBuilder
					builder ++= iterator
					builder.result()
					
				// Case: Empty collection => No building is needed
				case None => empty
			}
	}
	
	override def apply(item: Int): IntSet = apply(Single(NumericSpan.singleValue(item)))
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param ordered A collection sorted in ascending order
	 * @return An int-set from that collection
	 */
	@throws[IllegalArgumentException]("If the specified collection is not ordered")
	def fromOrdered(ordered: IterableOnce[Int]) = ordered match {
		case i: IntSet => i
		case i => _fromOrdered(i)
	}
	
	private def _fromOrdered(orderedInput: IterableOnce[Int]) = {
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
			else if (i < currentEnd)
				throw new IllegalArgumentException("The specified iterator was not ordered")
		}
		builder += NumericSpan(currentStart, currentEnd)
		
		apply(builder.result())
	}
	
	
	// NESTED   ------------------------------
	
	/**
	 * Used for building IntSets, minimizing memory usage
	 */
	class IntSetBuilder(startingFrom: Option[IntSet] = None) extends mutable.Builder[Int, IntSet]
	{
		// ATTRIBUTES   ----------------------
		
		private val ranges = mutable.Buffer[RangeBuilder]()
		
		
		// INITIAL CODE ----------------------
		
		startingFrom.foreach { set => ranges ++= set.ranges.view.map(RangeBuilder.from) }
		
		
		// IMPLEMENTED  ----------------------
		
		override def addOne(elem: Int) = {
			// Finds the following range (index)
			val nextRangeIndex = ranges.indexWhere { _.start > elem }
			// Case: There is no following range
			if (nextRangeIndex == -1)
				ranges.lastOption match {
					case Some(lastRange) =>
						// Case: Not contained within the last range => Extends or adds
						if (lastRange.end < elem) {
							// Case: Just after the last range => Extends the last range
							if (lastRange.end == elem - 1)
								lastRange.end = elem
							// Case: Further after the last range => Adds a new range
							else
								ranges.append(new RangeBuilder(elem, elem))
						}
					
					// Case: This builder is empty => Adds the first range
					case None => ranges.append(new RangeBuilder(elem, elem))
				}
			// Case: This number is before the first range => Extends or adds
			else if (nextRangeIndex == 0) {
				val nextRange = ranges.head
				// Case: Just before the first range => Extends the first range
				if (nextRange.start == elem + 1)
					nextRange.start = elem
				// Case: Further before the first range => Adds a new range
				else
					ranges.insert(0, new RangeBuilder(elem, elem))
			}
			// Case: This number is somewhere within this builder
			else {
				val previousRange = ranges(nextRangeIndex - 1)
				val nextRange = ranges(nextRangeIndex)
				// Case: Not contained within the previous range => Extends or adds
				if (previousRange.end < elem) {
					// Case: Just after the previous range
					if (previousRange.end == elem - 1) {
						// Case: Also, just before the next range => Combines these two ranges
						if (nextRange.start == elem + 1) {
							ranges.remove(nextRangeIndex)
							previousRange.end = nextRange.end
						}
						// Case: Not just before the next range => Extends the previous range
						else
							previousRange.end = elem
					}
					// Case: Just before the next range => Extends the next range
					else if (nextRange.start == elem + 1)
						nextRange.start = elem
					// Case: Disconnected from surrounding ranges => Adds a new range
					else
						ranges.insert(nextRangeIndex, new RangeBuilder(elem, elem))
				}
			}
			this
		}
		
		override def clear() = ranges.clear()
		override def result() = new IntSet(ranges.view.map { _.toSpan }.toOptimizedSeq)
	}
	
	private object RangeBuilder
	{
		def from(range: HasInclusiveEnds[Int]) = new RangeBuilder(range.start, range.end)
	}
	private class RangeBuilder(var start: Int, var end: Int)
	{
		def toSpan = NumericSpan(start, end)
	}
}

/**
  * A set of integers, where consecutive sequences are stored as ranges.
  * @author Mikko Hilpinen
  * @since 30.07.2024, v2.5
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
			IntSet(ranges.mapHead { _.mapStart { _ + 1 } })
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
	
	override def filter(pred: Int => Boolean) = IntSet._fromOrdered(iterator.filter(pred))
	override def filterNot(pred: Int => Boolean) = IntSet._fromOrdered(iterator.filterNot(pred))
	
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
	override def takeWhile(p: Int => Boolean) = IntSet._fromOrdered(iterator.takeWhile(p))
	
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
	override def dropWhile(p: Int => Boolean) = IntSet._fromOrdered(iterator.dropWhile(p))
	
	
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
	 * @param values Integers to add to this set
	 * @return A copy of this set with all specified integers included
	 */
	def ++(values: IterableOnce[Int]) = {
		if (values.knownSize == 0)
			this
		else {
			val builder = new IntSetBuilder(Some(this))
			builder ++= values
			builder.result()
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
			IntSet(OptimizedIndexedSeq.concat(ranges.take(targetIndex), replacingRanges, ranges.drop(targetIndex + 1)))
			
		case None => this
	}
	/**
	 * @param values Values to remove from this set
	 * @return Copy of this set with the specified values removed
	 */
	def --(values: IterableOnce[Int]) = {
		if (values.knownSize == 0 || isEmpty)
			this
		else {
			values match {
				case v: View[Int] =>
					val values = Set.from(v)
					if (values.isEmpty)
						this
					else
						filterNot(values.contains)
					
				case i: Iterable[Int] =>
					if (i.isEmpty)
						this
					else
						i match {
							case s: Set[Int] => filterNot(s.contains)
							case s: IntSet => filterNot(s.contains)
							case s: Seq[Int] => filterNot(s.contains)
							case v =>
								val values = Set.from(v)
								filterNot(values.contains)
						}
						
				case i =>
					val values = Set.from(i)
					if (values.isEmpty)
						this
					else
						filterNot(values.contains)
			}
		}
	}
}