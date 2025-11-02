package utopia.flow.operator.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.HasInclusiveOrderedEnds
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.operator.enumeration.Extreme.FindExtreme
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}

/**
  * A common trait for the two extremes min/low and max/high.
  * @author Mikko Hilpinen
  * @since 1.2.2023, v2.0
  */
sealed trait Extreme extends Binary[Extreme]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return The end of a collection matching this extreme when no ordering is applied
	  */
	def toEnd: End
	
	/**
	 * @param first The first item to compare
	 * @param second The second item to compare
	 * @param ord Implicit ordering used
	 * @tparam A Type of the compared values
	 * @return
	 *      - 0, if the two values are equal
	 *      - >0, if the first value is more extreme than the second value
	 *      - <0, if the second value is more extreme than the first value
	 */
	def compare[A](first: A, second: A)(implicit ord: Ordering[A]): Int
	/**
	  * @param ascendingOrder An ordering that returns items in ascending order (i.e. from the smallest to the greatest)
	  * @tparam A Type of ordered items
	  * @return An ordering that returns items in order of extremity (i.e. from less extreme to more extreme)
	  */
	def ascendingToExtreme[A](ascendingOrder: Ordering[A]): Ordering[A]
	
	/**
	  * Selects the more extreme item from the two candidates
	  * @param first The first candidate
	  * @param second The second candidate
	  * @param ord Implicit ordering to apply
	  * @tparam A Type of candidates
	  * @return The more extreme candidate (on this side). The first candidate if the two are equal.
	  */
	def apply[A](first: A, second: A)(implicit ord: Ordering[A]): A
	
	/**
	  * @param coll A collection
	  * @param ord  Implicit ordering to use
	  * @tparam A Type of items within that collection
	  * @return The most extreme item from that collection on this side
	  * @throws NoSuchElementException If the specified collection is empty
	  */
	@throws[NoSuchElementException]("If the specified iterator is empty")
	def from[A](coll: IterableOnce[A])(implicit ord: Ordering[A]): A
	
	
	// COMPUTED -------------------------
	
	/**
	  * @param ord Implicit ordering to use
	  * @tparam A Type of searched items
	  * @return An instance that will utilize the specified ordering in order to find extreme items from collections
	  */
	def find[A](implicit ord: Ordering[A]) = FindExtreme[A](this)
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: Extreme = this
	
	
	// OTHER    ------------------------
	
	/**
	  * @param pair A pair of items
	  * @param ord Implicit ordering to use
	  * @tparam A Type of items in the pair
	  * @return The more extreme item from the two
	  */
	def from[A](pair: Pair[A])(implicit ord: Ordering[A]): A = apply(pair.first, pair.second)
	/**
	  * @param ended An item with two ends
	  * @tparam A Type of ends in that item
	  * @return The end that corresponds with this extreme
	  */
	def from[A](ended: HasInclusiveOrderedEnds[A]): A = apply(ended.start, ended.end)(ended.ordering)
	
	/**
	  * @param first  An item
	  * @param second Another item
	  * @param third  Another item
	  * @param more   More items
	  * @param ord    Implicit ordering to use
	  * @tparam A Type of compared items
	  * @return The most extreme item from the specified set of items
	  */
	def apply[A](first: A, second: A, third: A, more: A*)(implicit ord: Ordering[A]) =
		from(Vector(first, second, third) ++ more)
	
	/**
	  * @param coll A collection
	  * @param ord Implicit ordering to use
	  * @tparam A Type of items within that collection
	  * @return The most extreme item from that collection on this side. None if the collection is empty.
	  */
	def findFrom[A](coll: IterableOnce[A])(implicit ord: Ordering[A]): Option[A] = coll match {
		case c: Iterable[A] => if (c.isEmpty) None else Some(from(c))
		case c =>
			val iter = c.iterator
			if (iter.hasNext)
				Some(from(iter))
			else
				None
	}
	@deprecated("Renamed to findFrom(IterableOnce)", "v2.7.1")
	def optionFrom[A](coll: IterableOnce[A])(implicit ord: Ordering[A]): Option[A] = findFrom(coll)
	
	/**
	  * @param f A mapping function
	  * @param ord Implicit ordering for the map results
	  * @tparam A Type of searched items
	  * @tparam B Type of map results
	  * @return An instance that may be used to find items of this extreme from collections
	  */
	def by[A, B](f: A => B)(implicit ord: Ordering[B]) = FindExtreme.by(this)(f)
}

object Extreme
{
	// ATTRIBUTES   ---------------------
	
	/**
	  * The minimum and the maximum extreme
	  */
	val values = Pair[Extreme](Min, Max)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param sign Targeted sign
	  * @return The extreme that matches that sign (i.e. Min for Negative and Max for Positive)
	  */
	def apply(sign: Sign): Extreme = sign match {
		case Positive => Max
		case Negative => Min
	}
	
	
	// VALUES   -------------------------
	
	/**
	  * The minimum / the low extreme
	  */
	case object Min extends Extreme
	{
		override def toEnd: End = First
		
		override def unary_- = Max
		override def compareTo(o: Extreme) = if (o == this) 0 else -1
		
		override def compare[A](first: A, second: A)(implicit ord: Ordering[A]): Int = -ord.compare(first, second)
		override def ascendingToExtreme[A](ascendingOrder: Ordering[A]): Ordering[A] = ascendingOrder.reverse
		
		def apply[A](first: A, second: A)(implicit ord: Ordering[A]): A = ord.min(first, second)
		
		override def from[A](coll: IterableOnce[A])(implicit ord: Ordering[A]) = coll match {
			case i: Iterable[A] => i.min
			case i => i.iterator.min
		}
	}
	/**
	  * The maximum / the high extreme
	  */
	case object Max extends Extreme
	{
		override def toEnd: End = Last
		
		override def unary_- = Min
		override def compareTo(o: Extreme) = if (o == this) 0 else 1
		
		override def compare[A](first: A, second: A)(implicit ord: Ordering[A]): Int = ord.compare(first, second)
		override def ascendingToExtreme[A](ascendingOrder: Ordering[A]): Ordering[A] = ascendingOrder
		
		def apply[A](first: A, second: A)(implicit ord: Ordering[A]): A = ord.max(first, second)
		
		override def from[A](coll: IterableOnce[A])(implicit ord: Ordering[A]) = coll match {
			case i: Iterable[A] => i.max
			case i => i.iterator.max
		}
	}
	
	
	// NESTED   ----------------------------
	
	object FindExtreme
	{
		// OTHER    -------------------------
		
		/**
		 * @param extreme Targeted extreme
		 * @param ord Implicit ordering
		 * @tparam A Type of the compared values
		 * @return An interface for finding the most extreme value
		 */
		def apply[A](extreme: Extreme)(implicit ord: Ordering[A]): FindExtreme[A] = new _FindExtreme(extreme)
		/**
		 * @param extreme Targeted extreme
		 * @param f A function for converting an item into a comparable key
		 * @param ord Implicit ordering used
		 * @tparam A Type of the compared items
		 * @tparam K Type of the keys formed from the items
		 * @return An interface for finding the most extreme value
		 */
		def by[A, K](extreme: Extreme)(f: A => K)(implicit ord: Ordering[K]): FindExtreme[A] =
			new FindExtremeBy[A, K](extreme)(f)
		
		
		// NESTED   -------------------------
		
		private class _FindExtreme[A](extreme: Extreme)(implicit ord: Ordering[A]) extends FindExtreme[A]
		{
			override def from(coll: IterableOnce[A]): A = extreme.from(coll)
			override def from(first: A, second: A): A = extreme(first, second)
			override def findFrom(coll: IterableOnce[A]): Option[A] = extreme.findFrom(coll)
		}
		
		private class FindExtremeBy[V, K](extreme: Extreme)(f: V => K)(implicit ord: Ordering[K])
			extends FindExtreme[V]
		{
			override def from(first: V, second: V): V = {
				val comparison = extreme.compare(f(first), f(second))
				if (comparison >= 0)
					first
				else
					second
			}
			
			override def from(coll: IterableOnce[V]): V = {
				val iterator = coll.iterator
				var best = iterator.next()
				var bestKey = f(best)
				
				while (iterator.hasNext) {
					val next = iterator.next()
					val nextKey = f(next)
					if (extreme.compare(nextKey, bestKey) > 0) {
						best = next
						bestKey = nextKey
					}
				}
				
				best
			}
			override def findFrom(coll: IterableOnce[V]): Option[V] = {
				val iterator = coll.iterator
				if (iterator.hasNext)
					Some(from(iterator))
				else
					None
			}
		}
	}
	trait FindExtreme[A]
	{
		// ABSTRACT -------------------------
		
		/**
		 * @param coll A collection
		 * @return The most extreme item from that collection on this side
		 * @throws NoSuchElementException If the specified collection is empty
		 */
		@throws[NoSuchElementException]("If the specified collection is empty")
		def from(coll: IterableOnce[A]): A
		/**
		 * Finds the more extreme from the specified two items
		 * @param first The first item
		 * @param second The second item
		 * @return The more extreme of the two items
		 */
		def from(first: A, second: A): A
		/**
		 * @param coll A collection
		 * @return The most extreme item from that collection on this side. None if the collection is empty.
		 */
		def findFrom(coll: IterableOnce[A]): Option[A]
		
		
		// OTHER    -----------------------
		
		/**
		 * @param pair A pair of values
		 * @return The more extreme of the two items
		 */
		def from(pair: Pair[A]): A = from(pair.first, pair.second)
		/**
		 * @param first  An item
		 * @param second Another item
		 * @param third  Another item
		 * @param more   More items
		 * @return The most extreme item from the specified set of items
		 */
		def from(first: A, second: A, third: A, more: A*): A = from(Vector(first, second, third) ++ more)
		
		@deprecated("Renamed to findFrom(IterableOnce)", "v2.7.1")
		def optionFrom(coll: IterableOnce[A]): Option[A] = findFrom(coll)
	}
}
