package utopia.flow.collection.immutable

import utopia.flow.collection.mutable.iterator.ZipPadIterator
import utopia.flow.operator.{EqualsFunction, Sign}
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.annotation.switch
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.VectorBuilder
import scala.collection.{IndexedSeqOps, mutable}
import scala.language.implicitConversions

object Pair
{
	// IMPLICIT ----------------------------------
	
	implicit def tupleToPair[A](tuple: (A, A)): Pair[A] = apply(tuple._1, tuple._2)
	implicit def pairToTuple[A](pair: Pair[A]): (A, A) = pair.toTuple
	
	implicit class PairOfDoubles(val pair: Pair[Double]) extends AnyVal
	{
		/**
		  * @return Difference between the two values in this pair
		  */
		def diff = pair.second - pair.first
	}
	
	implicit class PairOfInts(val pair: Pair[Int]) extends AnyVal
	{
		/**
		  * @return Difference between the two values in this pair
		  */
		def diff = pair.second - pair.first
	}
	
	
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
	
	implicit class RichCollPair[A](val p: Pair[Iterable[A]]) extends AnyVal
	{
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
// Currently cannot extend Reversible because of repr conflict in Iterable
case class Pair[+A](first: A, second: A) extends IndexedSeq[A] with IndexedSeqOps[A, IndexedSeq, IndexedSeq[A]] // with Reversible[Pair[A]]
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
	
	def unary_- = reverse
	
	/**
	  * @return Whether the two values in this pair are equal
	  */
	def isSymmetric = first == second
	/**
	  * @return Whether the two values in this pair are not equal
	  */
	def isNotSymmetric = !isSymmetric
	
	
	// IMPLEMENTED  ----------------------
	
	// Cannot implement this because repr is final (and deprecated) in IterableOps for some reason (sheesh...)
	// override def repr = this
	
	override def iterator: Iterator[A] = new PairIterator
	
	override def length = 2
	
	override def apply(index: Int) = (index: @switch) match
	{
		case 0 => first
		case 1 => second
		case _ => throw new IndexOutOfBoundsException(s"Attempting to access index $index of a pair")
	}
	
	override def reverse = Pair(second, first)
	
	override def toVector = Vector(first, second)
	
	override def map[B](f: A => B) = Pair(f(first), f(second))
	
	override def reduce[B >: A](op: (B, B) => B) = merge(op)
	
	override def toString() = s"($first, $second)"
	
	override def isTraversableAgain = true
	
	override def head = first
	
	override def headOption = Some(first)
	
	override def last = second
	
	override def lastOption = Some(second)
	
	override def isEmpty = false
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) = Vector.from(coll)
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, Vector[A]] = new VectorBuilder[A]()
	
	override def empty = Vector.empty[A]
	
	protected override def reversed = reverse
	
	override def knownSize = 2
	
	/**
	  * @param item An item
	  * @return Whether this pair contains that specific item
	  */
	override def contains[B >: A](item: B) = first == item || second == item
	
	override def sorted[B >: A](implicit ord: Ordering[B]) = {
		val cmp = ord.compare(first, second)
		if (cmp > 0) reverse else this
	}
	override def max[B >: A](implicit ord: Ordering[B]) = ord.max(first, second)
	override def maxBy[B](f: A => B)(implicit cmp: Ordering[B]) =
		if (cmp.gt(f(second), f(first))) second else first
	override def min[B >: A](implicit ord: Ordering[B]) = ord.min(first, second)
	override def minBy[B](f: A => B)(implicit cmp: Ordering[B]) =
		if (cmp.lt(f(second), f(first))) second else first
	
	
	// OTHER    --------------------------
	
	/**
	  * @param sign A sign
	  * @return This pair on Positive, reversed copy on Negative
	  */
	def *(sign: Sign) = sign match {
		case Positive => this
		case Negative => reverse
	}
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
	  * @param f A reduce function that accepts the second item, then the first item
	  * @tparam B Type of function result
	  * @return Function result
	  */
	def reverseReduce[B](f: (A, A) => B) = f(second, first)
	
	/**
	  * Combines this pair with another pair
	  * @param other Another pair
	  * @param f A merging function
	  * @tparam B Type of other pair
	  * @tparam C Type of merge result
	  * @return A combined pair
	  */
	def mergeWith[B, C](other: Pair[B])(f: (A, B) => C) =
		Pair(f(first, other.first), f(second, other.second))
	
	/**
	  * Merges this pair with another pair, resulting in a pair containing the entries from both
	  * @param other Another pair
	  * @tparam B Type of items in the other pair
	  * @return A pair that combines the values of both of these pairs in tuples
	  */
	def zip[B](other: Pair[B]) = Pair((first, other.first), (second, other.second))
	
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
	
	
	// NESTED   ------------------------------
	
	private class PairIterator extends Iterator[A]
	{
		// ATTRIBUTES   ----------------------
		
		var nextIndex = 0
		
		
		// IMPLEMENTED  ----------------------
		
		override def hasNext = nextIndex < 2
		
		override def next() =
		{
			nextIndex += 1
			nextIndex match
			{
				case 1 => first
				case 2 => second
				case _ => throw new IllegalStateException("next() called on consumed iterator")
			}
		}
	}
}
