package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.range.Span
import utopia.flow.collection.mutable.iterator.ZipPadIterator
import utopia.flow.operator.{Combinable, EqualsFunction, Reversible, Sign}
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.annotation.switch
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.VectorBuilder
import scala.collection.{IndexedSeqOps, mutable}
import scala.language.{implicitConversions, reflectiveCalls}

object Pair
{
	// IMPLICIT ----------------------------------
	
	implicit def tupleToPair[A](tuple: (A, A)): Pair[A] = apply(tuple._1, tuple._2)
	implicit def pairToTuple[A](pair: Pair[A]): (A, A) = pair.toTuple
	
	
	// OTHER    ------------------------------------
	
	/**
	  * @param item An item
	  * @tparam A Type of that item
	  * @return That item twice
	  */
	def twice[A](item: A) = apply(item, item)
	
	/**
	  * @param item An item or function that will be called twice (call-by-name)
	  * @tparam A Type of item stored in this pair
	  * @return A new pair with two values of the specified function
	  */
	def fill[A](item: => A) = apply(item, item)
	
	
	// EXTENSIONS ----------------------------------
	
	implicit class SummingPair[A <: Combinable[A, A]](val p: Pair[A]) extends AnyVal
	{
		/**
		  * @return The sum of the items in this pair
		  */
		def sum = p.first + p.second
	}
	
	implicit class DifferencePair[A <: Combinable[A, R] with Reversible[A], R](val p: Pair[A]) extends AnyVal
	{
		/**
		  * @return The difference between the items in this pair
		  */
		def diff = p.second - p.first
	}
	
	implicit class NumericPair[N](val p: Pair[N])(implicit n: Numeric[N])
	{
		/**
		  * @return Sum of the values in this pair
		  */
		def sum = n.plus(p.first, p.second)
		/**
		  * @return Difference between the values in this pair (second - first)
		  */
		def diff = n.minus(p.second, p.first)
		/**
		  * @param mod A multiplier
		  * @return A multiplied copy of this pair
		  */
		def *(mod: N) = p.map { n.times(_, mod) }
	}
	
	implicit class RichCollPair[A](val p: Pair[Iterable[A]]) extends AnyVal
	{
		/*
		/**
		  * @return An iterator that returns items from both sides of this pair.
		  *         The items in the first (left) collection are all returned
		  *         before items from the second (right) collection.
		  */
		def flatIterator = p.first.iterator ++ p.second.iterator
		/**
		  * @return An iterator that returns items from both sides of this pair,
		  *         including the side on which that item appears (Negative = first / left, Positive = second / right).
		  *         The items in the first (left) collection are all returned
		  *         before items from the second (right) collection.
		  */
		def flatIteratorWithSides =
			p.first.iterator.map { _ -> Negative } ++ p.second.iterator.map { _ -> Positive }
		 */
		
		/**
		  * @return Zips the two collections in this pair together.
		  *         (but only on the overlapping area, i.e. the common size)
		  */
		def zipMerge = p.merge { _ zip _ }
		/**
		  * @return An iterator that zips the two collections in this pair together
		  *         (but only on the overlapping area, i.e. the common size)
		  */
		def zipIterator = p.merge { _.iterator zip _.iterator }
		/**
		  * @param pad A function that yields items to place the shorter collection
		  * @return An iterator that zips all items on both sides of this pair,
		  *         padding the shorter collection where/if necessary
		  */
		def zipPadIterator(pad: => A) = ZipPadIterator[A](p.first.iterator, p.second.iterator, pad)
		/**
		  * @param pad Asymmetric padding to apply, where the left side contains the padding for the
		  *            first collection and the right side contains padding for the second collection.
		  * @return An iterator that zips all items on both sides of this pair,
		  *         padding the shorter collection where/if necessary
		  */
		def zipPadIterator(pad: Pair[A]) = ZipPadIterator[A, A](p.first.iterator, p.second.iterator, pad.first, pad.second)
		
		/**
		  * Groups the collections in this pair to 3 groups:
		  * 1: Items that appear only in the first collection
		  * 2: Items that appear only in the second collection
		  * 3: Items that appear in both collections, based on the specified matching function
		  *
		  * Assumes the items in each collection to be distinct.
		  * If the items in the second collection are not distinct, this function may behave in an unpredictable manner
		  * (i.e. an item from the second collection may appear in both groups 2 and 3).
		  *
		  * @param equals A function that determines whether the two items form a "match",
		  *               and should therefore be placed together in group 3.
		  *
		  * @return Groups 1 and 2 as a Pair of Sets + group 3 as a Vector of Pairs.
		  *         The ordering of group 3 is dictated by the ordering in the second source collection.
		  *         The ordering in groups 1 and 2 is lost (obviously).
		  */
		def separateMatching(implicit equals: EqualsFunction[A]) =
			separateMatchingWith(equals.apply)
		/**
		  * Groups the collections in this pair to 3 groups:
		  * 1: Items that appear only in the first collection
		  * 2: Items that appear only in the second collection
		  * 3: Items that appear in both collections, based on the specified matching function
		  *
		  * Assumes the items in each collection to be distinct.
		  * If the items in the second collection are not distinct, this function may behave in an unpredictable manner
		  * (i.e. an item from the second collection may appear in both groups 2 and 3).
		  *
		  * @param f A function that determines whether the two items form a "match",
		  *          and should therefore be placed together in group 3.
		  *
		  * @return Groups 1 and 2 as a Pair of Sets + group 3 as a Vector of Pairs.
		  *         The ordering of group 3 is dictated by the ordering in the second source collection.
		  *         The ordering in groups 1 and 2 is lost (obviously).
		  */
		def separateMatchingWith(f: (A, A) => Boolean) = {
			val firstUniquePool = mutable.Set.from[A](p.first)
			val secondUniqueBuilder = mutable.Set[A]()
			val matchesBuilder = new VectorBuilder[Pair[A]]()
			
			p.second.foreach { b =>
				firstUniquePool.find { a => f(a, b) } match {
					case Some(a) =>
						firstUniquePool -= a
						matchesBuilder += Pair(a, b)
					case None => secondUniqueBuilder += b
				}
			}
			
			Pair(firstUniquePool.toSet, secondUniqueBuilder.toSet) -> matchesBuilder.result()
		}
	}
}

/**
 * A struct holding two values of the same type
 * @author Mikko Hilpinen
 * @since 21.9.2021, v1.12
 */
case class Pair[+A](first: A, second: A)
	extends IndexedSeq[A] with IndexedSeqOps[A, IndexedSeq, IndexedSeq[A]] with Reversible[Pair[A]]
{
	// COMPUTED --------------------------
	
	/**
	 * @return A tuple based on this pair
	 */
	def toTuple = first -> second
	/**
	 * @return A map with the same contents with this pair (first item linked with Negative, second with Positive)
	 */
	def toMap = Map(Negative -> first, Positive -> second)
	
	/**
	  * @return Whether the two values in this pair are equal
	  */
	def isSymmetric = first == second
	/**
	  * @return Whether the two values in this pair are not equal
	  */
	def isAsymmetric = !isSymmetric
	/**
	  * @return Whether the two values in this pair are not equal
	  */
	@deprecated("Please use .isAsymmetric instead", "v2.0")
	def isNotSymmetric = !isSymmetric
	
	/**
	  * @return An iterator that returns values in this pair, along with the sides on which those values appear.
	  *         Negative represents the left / first side, Positive represents the right / second side.
	  */
	def iteratorWithSides = iterator.zip(Sign.values)
	
	/**
	  * @param ord Implicit ordering to apply
	  * @tparam B Type of ordered values
	  * @return A span from the first value of this pair to the second value of this pair
	  */
	def toSpan[B >: A](implicit ord: Ordering[B]) = Span[B](first, second)
	
	
	// IMPLEMENTED  ----------------------
	
	override def self = this
	
	override def iterator: Iterator[A] = new PairIterator
	
	override def unary_- = reverse
	
	override def length = 2
	override def knownSize = 2
	override def isEmpty = false
	override def isTraversableAgain = true
	
	override def head = first
	override def headOption = Some(first)
	override def last = second
	override def lastOption = Some(second)
	
	override def reverse = Pair(second, first)
	protected override def reversed = reverse
	
	override def toVector = Vector(first, second)
	override def toString = s"($first, $second)"
	
	override def empty = Vector.empty[A]
	
	override def distinct = if (first == second) Vector(first) else this
	override def distinctBy[B](f: A => B) = if (f(first) == f(second)) Vector(first) else this
	
	override def sorted[B >: A](implicit ord: Ordering[B]): Pair[A] = {
		val cmp = ord.compare(first, second)
		if (cmp > 0) reverse else this
	}
	override def max[B >: A](implicit ord: Ordering[B]) = ord.max(first, second)
	override def min[B >: A](implicit ord: Ordering[B]) = ord.min(first, second)
	
	override def apply(index: Int) = (index: @switch) match {
		case 0 => first
		case 1 => second
		case _ => throw new IndexOutOfBoundsException(s"Attempting to access index $index of a pair")
	}
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) = Vector.from(coll)
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, Vector[A]] = new VectorBuilder[A]()
	
	override def contains[B >: A](item: B) = first == item || second == item
	
	override def map[B](f: A => B) = Pair(f(first), f(second))
	
	override def reduce[B >: A](op: (B, B) => B) = merge(op)
	
	override def maxBy[B](f: A => B)(implicit cmp: Ordering[B]) =
		if (cmp.gt(f(second), f(first))) second else first
	override def minBy[B](f: A => B)(implicit cmp: Ordering[B]) =
		if (cmp.lt(f(second), f(first))) second else first
	
	
	// OTHER    --------------------------
	
	/**
	 * @param side A side (Positive = left = first, Negative = right = second)
	 * @return The item of this pair from that side
	 */
	def apply(side: Sign) = side match {
		case Positive => first
		case Negative => second
	}
	
	/**
	 * @param item An item
	 * @return The side (Negative for left / first, Positive for right / second) on which that item resides
	 *         in this pair. None if that item is not in this pair.
	 */
	def sideOf[B >: A](item: B): Option[Sign] =
		if (item == first) Some(Negative) else if (item == second) Some(Positive) else None
	/**
	  * @param item An item
	  * @return The item opposite to the specified item.
	  *         None if the specified item didn't appear in this pair.
	  */
	def oppositeOf[B >: A](item: B) =
		if (item == first) Some(second) else if (item == second) Some(first) else None
	/**
	  * Finds the item opposite to one matching a condition.
	  * Works like find, except that this function returns the opposite item.
	  *
	  * I.e. if 'f' returns true for the first item, returns the second item;
	  * If 'f' returns false for the first item and true for the second item, returns the first item;
	  * If 'f' returns false for both items, returns None.
	  *
	  * @param f A find function for the targeted (not returned) item
	  * @return The item opposite to the item for which 'f' returned true.
	  *         None if 'f' returned false for both items.
	  */
	def oppositeToWhere(f: A => Boolean) =
		if (f(first)) Some(second) else if (f(second)) Some(first) else None
	
	/**
	  * @param newFirst New first item
	  * @tparam B Type of new first item
	  * @return A copy of this pair with the first item replaced
	  */
	def withFirst[B >: A](newFirst: B) = Pair(newFirst, second)
	/**
	  * @param newSecond New second item
	  * @tparam B Type of new second item
	  * @return Copy of this pair with the second item replaced
	  */
	def withSecond[B >: A](newSecond: B) = Pair(first, newSecond)
	/**
	  * @param newItem New item
	  * @param side Side on which the item will be placed
	  * @tparam B Type of the new item
	  * @return A copy of this pair with the specified item replacing one of the items in this pair
	  */
	def withItem[B >: A](newItem: B, side: Sign) = side match {
		case Negative => withFirst(newItem)
		case Positive => withSecond(newItem)
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
	  * @param side Targeted side (Negative = left / first, Positive = right / second)
	  * @param f A mapping function
	  * @tparam B Type of function result
	  * @return A copy of this pair with the targeted item mapped
	  */
	def mapSide[B >: A](side: Sign)(f: A => B) = side match {
		case Negative => mapFirst(f)
		case Positive => mapSecond(f)
	}
	/**
	  * @param f A mapping function that accepts a value from this pair, and the side on which that value appears.
	  *          Negative is the left / first side, Positive is the right / second side.
	  * @tparam B Type of mapping results
	  * @return A mapped copy of this pair
	  */
	def mapWithSides[B](f: (A, Sign) => B) = Pair(f(first, Negative), f(second, Positive))
	
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
	  * @tparam C Type of merge result
	  * @return A combined pair
	  */
	def mergeWith[B, C](other: Pair[B])(f: (A, B) => C) =
		Pair(f(first, other.first), f(second, other.second))
	/**
	  * Combines this pair with another pair, matching first and second items together
	  * @param other Another pair
	  * @param f A merge function that accepts a value from both pairs.
	  *          The values are always acquired from the same side on both pairs.
	  *          Returns 0-n items.
	  * @tparam B Type of values in the other pair
	  * @tparam C Type of individual merge results
	  * @return All merge results as a vector
	  */
	def flatMergeWith[B, C](other: Pair[B])(f: (A, B) => IterableOnce[C]) = {
		val builder = new VectorBuilder[C]()
		builder ++= f(first, other.first)
		builder ++= f(second, other.second)
		builder.result()
	}
	/**
	 * @param other Another pair
	 * @param f A predicate that compares the values of these pairs
	 * @tparam B Type of values in the other pair
	 * @return Whether the specified predicate returns true for either side
	 */
	def existsWith[B](other: Pair[B])(f: (A, B) => Boolean) = f(first, other.first) || f(second, other.second)
	/**
	 * @param other Another pair
	 * @param f     A predicate that compares the values of these pairs
	 * @tparam B Type of values in the other pair
	 * @return Whether the specified predicate returns true for both sides
	 */
	def forallWith[B](other: Pair[B])(f: (A, B) => Boolean) = f(first, other.first) && f(second, other.second)
	/**
	  * Merges this pair with another pair, resulting in a pair containing the entries from both
	  * @param other Another pair
	  * @tparam B Type of items in the other pair
	  * @return A pair that combines the values of both of these pairs in tuples
	  */
	def zip[B](other: Pair[B]) = Pair((first, other.first), (second, other.second))
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
	
	/**
	  * Merges together the two values in this pair using a function.
	  * Works exactly like reduce.
	  * @param f A function that accepts the two values in this pair and yields a merge value
	  * @tparam B Function result type
	  * @return Function result
	  */
	def merge[B](f: (A, A) => B) = f(first, second)
	/**
	  * Compares the two values in this pair using the specified function
	  * @param f A function for comparing two values with each other
	  * @tparam B Type of function result
	  * @return Function result
	  */
	@deprecated("Renamed to .merge(...)", "v2.0")
	def compareWith[B](f: (A, A) => B) = merge(f)
	
	/**
	  * @param e An equals function
	  * @return Whether the two values of this pair are equal when applying the specified function
	  */
	def equalsUsing(e: EqualsFunction[A]) = e.equals(first, second)
	/**
	  * @param e An equals function
	  * @return Whether the two values of this pair are unequal when applying the specified function
	  */
	def notEqualsUsing(e: EqualsFunction[A]) = e.not(first, second)
	/**
	  * @param f A mapping function
	  * @tparam B Type of mapping results
	  * @return Whether the values in this pair are symmetric (i.e. equal) after applying the specified mapping function
	  */
	def isSymmetricBy[B](f: A => B) = merge { f(_) == f(_) }
	/**
	  * @param f A mapping function
	  * @tparam B Type of mapping result
	  * @return Whether the values in this pair are asymmetric (i.e. not equal)
	  *         after applying the specified mapping function
	  */
	def isAsymmetricBy[B](f: A => B) = !isSymmetricBy(f)
	
	/**
	  * @param other Another pair
	  * @param eq Implicit equals function to use
	  * @tparam B Type of values in the other pair
	  * @return Whether these pairs are equal when using the specified function
	  */
	def ~==[B >: A](other: Pair[B])(implicit eq: EqualsFunction[B]) =
		eq(first, other.first) && eq(second, other.second)
	/**
	  * @param other Another pair
	  * @param eq    Implicit equals function to use
	  * @tparam B Type of values in the other pair
	  * @return Whether these pairs are not equal when using the specified function
	  */
	def !~==[B >: A](other: Pair[B])(implicit eq: EqualsFunction[B]) = !(this ~== other)
	
	
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
