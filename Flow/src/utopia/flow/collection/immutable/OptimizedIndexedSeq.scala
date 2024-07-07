package utopia.flow.collection.immutable

import utopia.flow.view.mutable.caching.ResettableLazy

import scala.collection.immutable.VectorBuilder
import scala.collection.{BuildFrom, SeqFactory, View, mutable}

/**
  * Factory for constructing indexed sequences with following underlying classes:
  * - Empty (0 items)
  * - Single (1 item)
  * - Pair (2 items)
  * - Vector (> 2 items)
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
object OptimizedIndexedSeq extends SeqFactory[IndexedSeq]
{
	// IMPLEMENTED  ------------------------
	
	override def empty[A]: IndexedSeq[A] = Empty
	override def newBuilder[A]: mutable.Builder[A, IndexedSeq[A]] = new OptimizedSeqBuilder[A]()
	
	override def from[A](source: IterableOnce[A]): IndexedSeq[A] = source match {
		// Case: Already an indexed sequence => Won't transform
		case i: IndexedSeq[A] => i
		// Case: Other collection type
		case source =>
			// Checks whether the size of the collection is known
			source.knownSize match {
				// Case: 2 items => Yields a Pair
				case 2 =>
					val iter = source.iterator
					Pair.fill(iter.next())
				// Case: 1 item => Yields a Single
				case 1 => Single(source.iterator.next())
				// Case: Empty => Yields Empty
				case 0 => Empty
				// Case: More than 2 items => Yields a Vector
				case x if x > 2 => Vector.from(source)
				// Case: Unknown number of items
				case _ =>
					source match {
						case v: View[A] =>
							// WET WET
							val iter = v.iterator
							if (iter.hasNext) {
								val builder = new OptimizedSeqBuilder[A]()
								builder ++= iter
								builder.result()
							}
							else
								Empty
							
						// Case: Iterable collection => Utilizes isEmpty
						case i: Iterable[A] =>
							if (i.isEmpty)
								Empty
							else {
								val builder = new OptimizedSeqBuilder[A]()
								builder ++= i
								builder.result()
							}
						// Case: Only iterable once => Utilizes the collection iterator and uses a builder
						case source =>
							val iter = source.iterator
							if (iter.hasNext) {
								val builder = new OptimizedSeqBuilder[A]()
								builder ++= iter
								builder.result()
							}
							else
								Empty
					}
			}
	}
	
	override def iterate[A](start: A, len: Int)(f: A => A) = len match {
		case 2 => Pair(start, f(start))
		case 1 => Single(start)
		case x if x <= 0 => Empty
		case _ => Vector.iterate(start, len)(f)
	}
	override def fill[A](n: Int)(elem: => A) = n match {
		case 2 => Pair.fill(elem)
		case 1 => Single(elem)
		case x if x <= 0 => Empty
		case _ => Vector.fill(n)(elem)
	}
	override def tabulate[A](n: Int)(f: Int => A) = n match {
		case 2 => Pair(f(0), f(1))
		case 1 => Single(f(0))
		case x if x <= 0 => Empty
		case _ => Vector.tabulate(n)(f)
	}
	
	
	// NESTED   ---------------------------
	
	class BuildOptimizedSeqFrom[-From, A] extends BuildFrom[From, A, IndexedSeq[A]]
	{
		override def fromSpecific(from: From)(it: IterableOnce[A]): IndexedSeq[A] = OptimizedIndexedSeq.from(it)
		
		override def newBuilder(from: From): mutable.Builder[A, IndexedSeq[A]] = OptimizedIndexedSeq.newBuilder
	}
	
	/**
	  * A builder class that builds a Pair if the input is exactly two items.
	  * Otherwise builds a Vector.
	  * @tparam A Type of items placed in the resulting collection.
	  */
	// WET WET from PairOrVectorBuilder (consider removing that class or adding a common extension)
	class OptimizedSeqBuilder[A] extends mutable.Builder[A, IndexedSeq[A]]
	{
		// ATTRIBUTES   -------------------
		
		// Only tracked up to 3. After 3, the index doesn't matter anymore.
		private var nextIndex = 0
		private var first: Option[A] = None
		private var second: Option[A] = None
		
		// Used once number of items reaches 3
		private val lazyBuilder = ResettableLazy {
			val builder = new VectorBuilder[A]()
			// Adds the then-queued first and second item
			builder ++= first
			builder ++= second
			builder
		}
		
		
		// COMPUTED -----------------------
		
		private def overflown = nextIndex > 2
		
		
		// IMPLEMENTED  -------------------
		
		// Tracks size until 3
		override def knownSize = if (overflown) -1 else nextIndex
		
		override def addOne(elem: A) = {
			nextIndex match {
				case 0 => first = Some(elem)
				case 1 => second = Some(elem)
				case _ => lazyBuilder.value.addOne(elem)
			}
			if (nextIndex < 3)
				nextIndex += 1
			this
		}
		override def addAll(xs: IterableOnce[A]) = {
			// Case: Already building a vector => Delegates building
			if (overflown)
				lazyBuilder.value.addAll(xs)
			else {
				xs match {
					// Case: View => Converts to an iterator
					case v: View[A] =>
						val iter = v.iterator
						if (iter.hasNext)
							addFrom(iter)
					// Case: Iterable => Utilizes nonEmpty & knownSize
					case i: Iterable[A] =>
						if (i.nonEmpty) {
							val count = i.knownSize
							// Case: Number of added items is known => Switches directly to vector-building if needed
							if (count > 0) {
								val resultingIndex = nextIndex + count
								// Case: Will overflow => Builds directly to the vector
								if (resultingIndex > 2) {
									nextIndex = 3
									lazyBuilder.value.addAll(i)
								}
								// Case: Shouldn't overflow => Assigns the items normally
								else
									addFrom(i.iterator)
							}
							// Case: Number of items is unknown => Has to add one at a time
							else
								addFrom(i.iterator)
						}
					case i: Iterator[A] =>
						if (i.hasNext)
							addFrom(i)
					case i =>
						val iter = i.iterator
						if (iter.hasNext)
							addFrom(iter)
				}
			}
			this
		}
		
		override def clear() = {
			first = None
			second = None
			lazyBuilder.reset()
		}
		override def result() = nextIndex match {
			case 0 => Empty
			case 1 => Single(first.get)
			case 2 => Pair(first.get, second.get)
			case _ => lazyBuilder.value.result()
		}
		
		
		// OTHER    -----------------------
		
		// Assumes a non-empty iterator
		private def addFrom(iterator: Iterator[A]) = {
			nextIndex match {
				case 0 =>
					first = Some(iterator.next())
					if (iterator.hasNext) {
						second = Some(iterator.next())
						nextIndex = 2
					}
					else
						nextIndex = 1
				case 1 =>
					second = Some(iterator.next())
					nextIndex = 2
				case _ =>
					lazyBuilder.value.addAll(iterator)
					nextIndex = 3
			}
			
			if (iterator.hasNext) {
				lazyBuilder.value.addAll(iterator)
				nextIndex = 3
			}
		}
	}
}
