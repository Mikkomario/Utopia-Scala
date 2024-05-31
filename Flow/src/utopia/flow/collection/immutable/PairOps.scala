package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.range.Span
import utopia.flow.operator.Reversible
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.view.template.HasTwoSides

import scala.annotation.switch
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.VectorBuilder
import scala.collection.{IndexedSeqOps, mutable}

/**
 * Common trait for data structures that hold two values of the same type
 * @author Mikko Hilpinen
 * @since 21.9.2021, v2.3
  * @tparam A Type of values stored in this pair
  * @tparam CC Type of the collection constructor used for variable types (and lengths)
  * @tparam C Type of collection constructor used for collections of same type (but maybe different length)
  * @tparam P Type of collection constructor when length (2) is preserved but when type may be variable
  * @tparam Repr Type of this collection when same length and value type are preserved
 */
trait PairOps[+A, +CC[X] <: Iterable[X], +C <: Iterable[A], +P[X] <: CC[X], +Repr <: C]
	extends IndexedSeqOps[A, CC, C] with HasTwoSides[A] with Reversible[Repr]
{
	// ABSTRACT --------------------------
	
	protected def _empty: C
	
	protected def only(side: End): C
	protected def newPair[B](first: => B, second: => B): P[B]
	
	protected def _fromSpecific(coll: IterableOnce[A @uncheckedVariance]): C
	protected def _newSpecificBuilder: mutable.Builder[A @uncheckedVariance, C]
	
	
	// COMPUTED --------------------------
	
	/**
	  * @param ord Implicit ordering to apply
	  * @tparam B Type of ordered values
	  * @return A span from the first value of this pair to the second value of this pair
	  */
	def toSpan[B >: A](implicit ord: Ordering[B]) = Span[B](first, second)
	
	/**
	  * @param ord Implicit ordering to use
	  * @tparam B Type of compared values
	  * @return This pair ordered with the specified ordering, so that the smaller item appears before the larger item
	  */
	def minMax[B >: A](implicit ord: Ordering[B]) = if (ord.lt(first, second)) -this else self
	
	
	// IMPLEMENTED  ----------------------
	
	override def iterator: Iterator[A] = new PairIterator
	
	override def length = 2
	override def knownSize = 2
	override def isEmpty = false
	override def isTraversableAgain = true
	
	override def head = first
	override def headOption = Some(first)
	override def last = second
	override def lastOption = Some(second)
	
	override def reverse = -this
	protected override def reversed = reverse
	
	override def toVector = Vector(first, second)
	override def toString = s"($first, $second)"
	
	override def empty = _empty
	
	override def distinct = if (first == second) only(First) else self
	override def distinctBy[B](f: A => B) = if (f(first) == f(second)) only(First) else self
	
	override def sorted[B >: A](implicit ord: Ordering[B]): Repr = {
		val cmp = ord.compare(first, second)
		if (cmp > 0) reverse else self
	}
	override def sortWith(lt: (A, A) => Boolean) = if (lt(first, second)) reverse else self
	override def sortBy[B](f: A => B)(implicit ord: Ordering[B]) =
		if (ord.lt(f(first), f(second))) reverse else self
	
	override def max[B >: A](implicit ord: Ordering[B]) = super[HasTwoSides].max[B]
	override def min[B >: A](implicit ord: Ordering[B]) = super[HasTwoSides].min[B]
	
	override def apply(index: Int) = (index: @switch) match {
		case 0 => first
		case 1 => second
		case _ => throw new IndexOutOfBoundsException(s"Attempting to access index $index of a pair")
	}
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) = _fromSpecific(coll)
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, C] = _newSpecificBuilder
	
	override def foreach[U](f: A => U) = {
		f(first)
		f(second)
	}
	override def forall(p: A => Boolean) = p(first) && p(second)
	override def exists(p: A => Boolean) = p(first) || p(second)
	override def tail = only(Last)
	override def contains[B >: A](item: B) = first == item || second == item
	
	override def map[B](f: A => B): P[B] = newPair(f(first), f(second))
	
	override def reduce[B >: A](op: (B, B) => B) = merge(op)
	
	override def maxBy[B](f: A => B)(implicit cmp: Ordering[B]) =
		if (cmp.gt(f(second), f(first))) second else first
	override def minBy[B](f: A => B)(implicit cmp: Ordering[B]) =
		if (cmp.lt(f(second), f(first))) second else first
	
	override def zip[B](other: HasTwoSides[B]) = super[HasTwoSides].zip(other)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param f A mapping function that determines ordering
	  * @param ord Implicit ordering for mapping results
	  * @tparam B Type of mapping results
	  * @return A copy of this pair that is ordered so that the smaller item (according to the mapping result)
	  *         appears before the larger item.
	  */
	def minMaxBy[B](f: A => B)(implicit ord: Ordering[B]) = minMax(Ordering.by(f))
	
	/**
	  * @param newFirst New first item
	  * @tparam B Type of new first item
	  * @return A copy of this pair with the first item replaced
	  */
	def withFirst[B >: A](newFirst: B) = newPair(newFirst, second)
	/**
	  * @param newSecond New second item
	  * @tparam B Type of new second item
	  * @return Copy of this pair with the second item replaced
	  */
	def withSecond[B >: A](newSecond: B) = newPair(first, newSecond)
	/**
	  * @param newItem New item
	  * @param side Side on which the item will be placed
	  * @tparam B Type of the new item
	  * @return A copy of this pair with the specified item replacing one of the items in this pair
	  */
	def withSide[B >: A](newItem: B, side: End) = side match {
		case First => withFirst(newItem)
		case Last => withSecond(newItem)
	}
	
	/**
	  * @param f A mapping function
	  * @tparam B Type of function result
	  * @return A copy of this pair with the first item mapped
	  */
	def mapFirst[B >: A](f: A => B) = withFirst(f(first))
	/**
	  * @param f A mapping function
	  * @tparam B Type of function result
	  * @return A copy of this pair with the second item mapped
	  */
	def mapSecond[B >: A](f: A => B) = withSecond(f(second))
	/**
	  * @param side Targeted side
	  * @param f A mapping function
	  * @tparam B Type of function result
	  * @return A copy of this pair with the targeted item mapped
	  */
	def mapSide[B >: A](side: End)(f: A => B) = side match {
		case First => mapFirst(f)
		case Last => mapSecond(f)
	}
	/**
	  * @param f A mapping function that accepts a value from this pair, and the side on which that value appears.
	  *          Negative is the left / first side, Positive is the right / second side.
	  * @tparam B Type of mapping results
	  * @return A mapped copy of this pair
	  */
	def mapWithSides[B](f: (A, End) => B) = newPair(f(first, First), f(second, Last))
	
	/**
	  * @param f A reduce function that accepts the second item, then the first item
	  * @tparam B Type of function result
	  * @return Function result
	  */
	def reverseReduce[B](f: (A, A) => B) = f(second, first)
	
	/**
	  * Combines this pair with another pair, matching first and second items together
	  * @param other Another pair
	  * @param f A merge function that accepts a value from both pairs.
	  *          The values are always acquired from the same side on both pairs.
	  * @tparam B Type of other pair
	  * @tparam R Type of merge result
	  * @return A combined pair
	  */
	def mergeWith[B, R](other: HasTwoSides[B])(f: (A, B) => R) =
		newPair(f(first, other.first), f(second, other.second))
	/**
	  * Combines this pair with another pair, matching first and second items together
	  * @param other Another pair
	  * @param f A merge function that accepts a value from both pairs.
	  *          The values are always acquired from the same side on both pairs.
	  *          Returns 0-n items.
	  * @tparam B Type of values in the other pair
	  * @tparam R Type of individual merge results
	  * @return All merge results as a vector
	  */
	def flatMergeWith[B, R](other: HasTwoSides[B])(f: (A, B) => IterableOnce[R]) = {
		val builder = new VectorBuilder[R]()
		builder ++= f(first, other.first)
		builder ++= f(second, other.second)
		builder.result()
	}
	/**
	  * Attempts to merge this pair with another pair
	  * @param other Another pair
	  * @param f A merge function that combines values from both of these pairs.
	  *          Returns None in cases where merging is not possible.
	  *          Will be called 1-2 times.
	  * @tparam B Type of values in the other pair
	  * @tparam R Type of merged values, when successful
	  * @return A pair with two merge result values, or None if the specified function returned None at any point.
	  */
	def findMergeWith[B, R](other: HasTwoSides[B])(f: (A, B) => Option[R]) =
		f(first, other.first).flatMap { a => f(second, other.second).map { newPair(a, _) } }
	/**
	  * @param keys A pair of keys
	  * @tparam K Type of keys used
	  * @return A map where the specified set of pair are the keys and this pair are the values.
	  *         The mapping is based on ordering (first to first, second to second)
	  */
	def toMapWith[K](keys: Pair[K]) = keys.iterator.zip(this).toMap
	/**
	  * @param firstKey Key for the first (left) value in this pair
	  * @param secondKey Key for the second (right) value in this pair
	  * @tparam K Type of keys used
	  * @return A map where with the specified keys and this pair's values as values
	  */
	def toMapWith[K](firstKey: K, secondKey: K): Map[K, A] = toMapWith(Pair(firstKey, secondKey))
	
	
	// NESTED   ------------------------------
	
	private class PairIterator extends Iterator[A]
	{
		// ATTRIBUTES   ----------------------
		
		private var nextIndex = 0
		
		
		// IMPLEMENTED  ----------------------
		
		override def hasNext = nextIndex < 2
		
		override def next() = {
			nextIndex += 1
			nextIndex match {
				case 1 => first
				case 2 => second
				case _ => throw new IllegalStateException("next() called on consumed iterator")
			}
		}
	}
}
