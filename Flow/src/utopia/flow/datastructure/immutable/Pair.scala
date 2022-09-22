package utopia.flow.datastructure.immutable

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.annotation.switch
import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{IndexedSeqOps, mutable}
import scala.collection.immutable.VectorBuilder
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
	
	override def reduce[B >: A](op: (B, B) => B) = op(first, second)
	
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
	
	
	// OTHER    --------------------------
	
	/**
	  * @param sign A sign
	  * @return This pair on Positive, reversed copy on Negative
	  */
	def *(sign: Sign) = sign match
	{
		case Positive => this
		case Negative => reverse
	}
	
	/**
	 * @param side A side (Positive = left = first, Negative = right = second)
	 * @return The item of this pair from that side
	 */
	def apply(side: Sign) = side match
	{
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
	def withItem[B >: A](newItem: B, side: Sign) = side match
	{
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
	def mapSide[B >: A](side: Sign)(f: A => B) = side match
	{
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
	  * Compares the two values in this pair using the specified function
	  * @param f A function for comparing two values with each other
	  * @tparam B Type of function result
	  * @return Function result
	  */
	def compareWith[B](f: (A, A) => B) = f(first, second)
	
	
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
