package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.ZipPadIterator
import utopia.flow.operator.Reversible
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.equality.EqualsFunction
import utopia.flow.view.mutable.caching.ResettableLazy

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.VectorBuilder
import scala.collection.{SeqFactory, mutable}
import scala.language.{implicitConversions, reflectiveCalls}

object Pair
{
	// COMPUTED ----------------------------------
	
	/**
	  * @return A factory used for constructing Pairs or other indexed sequences, depending on the input size
	  */
	def factory = PairFactory
	
	
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
		def separateMatching(implicit equals: EqualsFunction[A] = EqualsFunction.default) =
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
	
	
	// NESTED   ---------------------------
	
	/**
	  * A builder class that builds a Pair if the input is exactly two items.
	  * Otherwise builds a Vector.
	  * @tparam A Type of items placed in the resulting collection.
	  */
	class PairOrVectorBuilder[A] extends mutable.Builder[A, Either[Vector[A], Pair[A]]]
	{
		// ATTRIBUTES   -------------------
		
		// Only tracked up to 3. After 3, the index doesn't matter anymore.
		private var nextIndex = 0
		private var first: Option[A] = None
		private var second: Option[A] = None
		
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
			case 0 => Left(Vector.empty)
			case 1 => Left(Vector(first.get))
			case 2 => Right(Pair(first.get, second.get))
			case _ => Left(lazyBuilder.value.result())
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
	
	object PairFactory extends SeqFactory[IndexedSeq]
	{
		override def empty[A]: Vector[A] = Vector.empty
		
		override def from[A](source: IterableOnce[A]): IndexedSeq[A] = source match {
			// Case: Already an indexed sequence => Won't transform
			case i: IndexedSeq[A] => i
			// Case: Other collection type
			case _ =>
				// Case: Known to contain two elements => Converts to a pair
				if (source.knownSize == 2) {
					val iter = source.iterator
					Pair(iter.next(), iter.next())
				}
				// Case: Other number of elements, or number of elements not known => Converts to an indexed seq
				else
					IndexedSeq.from(source)
		}
		
		override def newBuilder[A]: mutable.Builder[A, IndexedSeq[A]] =
			new PairOrVectorBuilder[A].mapResult { _.either }
	}
}

/**
 * A struct holding two values of the same type
 * @author Mikko Hilpinen
 * @since 21.9.2021, v1.12
 */
case class Pair[+A](first: A, second: A)
	extends PairOps[A, IndexedSeq, IndexedSeq[A], Pair, Pair[A]] with IndexedSeq[A]
{
	// IMPLEMENTED  ----------------------
	
	override def self = this
	override def iterableFactory = Pair.factory
	
	override def unary_- = Pair(second, first)
	override protected def _empty: IndexedSeq[A] = Vector.empty
	
	override protected def only(side: End): IndexedSeq[A] = Vector(apply(side))
	override protected def newPair[B](first: => B, second: => B): Pair[B] = Pair(first, second)
	
	override protected def _fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		Pair.factory.from(coll)
	override protected def _newSpecificBuilder: mutable.Builder[A @uncheckedVariance, IndexedSeq[A]] =
		Pair.factory.newBuilder
	
	
	// IMPLEMENTED  ----------------------
	
	override def view = new PairView[A](first, second)
	
	override def sorted[B >: A](implicit ord: Ordering[B]): Pair[A] = super[PairOps].sorted[B]
	
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) = _fromSpecific(coll)
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, IndexedSeq[A]] = _newSpecificBuilder
	
	override def filter(pred: A => Boolean) = {
		if (pred(first)) {
			if (pred(second))
				this
			else
				Vector(first)
		}
		else if (pred(second))
			Vector(second)
		else
			Vector.empty
	}
	
	
	// OTHER    --------------------------
	
	/**
	  * @param e An equals function
	  * @return Whether the two values of this pair are equal when applying the specified function
	  */
	@deprecated("Renamed to .isSymmetricWith(EqualsFunction)", "2.1")
	def equalsUsing(e: EqualsFunction[A]) = e(first, second)
	/**
	  * @param e An equals function
	  * @return Whether the two values of this pair are unequal when applying the specified function
	  */
	@deprecated("Renamed to .isAsymmetricWith(EqualsFunction)", "2.1")
	def notEqualsUsing(e: EqualsFunction[A]) = e.not(first, second)
}
