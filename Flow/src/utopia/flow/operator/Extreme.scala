package utopia.flow.operator

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.HasInclusiveOrderedEnds
import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * A common trait for the two extremes min/low and max/high.
  * @author Mikko Hilpinen
  * @since 1.2.2023, v2.0
  */
sealed trait Extreme extends Binary[Extreme]
{
	// ABSTRACT -------------------------
	
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
	  * @param iter An iterator
	  * @param ord  Implicit ordering to use
	  * @tparam A Type of items within that collection
	  * @return The most extreme item from that iterator (on this side)
	  * @throws NoSuchElementException If the specified iterator is empty
	  */
	@throws[NoSuchElementException]("If the specified iterator is empty")
	def from[A](iter: Iterator[A])(implicit ord: Ordering[A]): A
	/**
	  * @param coll A collection
	  * @param ord  Implicit ordering to use
	  * @tparam A Type of items within that collection
	  * @return The most extreme item from that collection on this side
	  * @throws NoSuchElementException If the specified collection is empty
	  */
	@throws[NoSuchElementException]("If the specified iterator is empty")
	def from[A](coll: Iterable[A])(implicit ord: Ordering[A]): A
	
	
	// COMPUTED -------------------------
	
	/**
	  * @param ord Implicit ordering to use
	  * @tparam A Type of searched items
	  * @return An instance that will utilize the specified ordering in order to find extreme items from collections
	  */
	def find[A](implicit ord: Ordering[A]) = new FindExtreme[A]()
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: Extreme = this
	
	
	// OTHER    ------------------------
	
	/**
	  * @param coll A collection
	  * @param ord  Implicit ordering to use
	  * @tparam A Type of items within that collection
	  * @return The most extreme item from that collection on this side
	  * @throws NoSuchElementException If the specified collection is empty
	  */
	@throws[NoSuchElementException]("If the specified collection is empty")
	def from[A](coll: IterableOnce[A])(implicit ord: Ordering[A]): A = coll match {
		case c: Iterable[A] => from(c)
		case c => from(c.iterator)
	}
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
	def optionFrom[A](coll: IterableOnce[A])(implicit ord: Ordering[A]): Option[A] = coll match {
		case c: Iterable[A] => if (c.isEmpty) None else Some(from(c))
		case c =>
			val iter = c.iterator
			if (iter.hasNext)
				Some(from(iter))
			else
				None
	}
	
	/**
	  * @param f A mapping function
	  * @param ord Implicit ordering for the map results
	  * @tparam A Type of searched items
	  * @tparam B Type of map results
	  * @return An instance that may be used to find items of this extreme from collections
	  */
	def by[A, B](f: A => B)(implicit ord: Ordering[B]) = new FindExtreme[A]()(Ordering.by[A, B](f))
	
	
	// NESTED   -----------------------
	
	class FindExtreme[A](implicit ord: Ordering[A])
	{
		/**
		  * @param coll A collection
		  * @return The most extreme item from that collection on this side
		  * @throws NoSuchElementException If the specified collection is empty
		  */
		@throws[NoSuchElementException]("If the specified collection is empty")
		def from(coll: IterableOnce[A]): A = Extreme.this.from(coll)(ord)
		/**
		  * Finds the more extreme from the specified two items
		  * @param first The first item
		  * @param second The second item
		  * @return The more extreme of the two items
		  */
		def from(first: A, second: A): A = Extreme.this(first, second)
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
		
		/**
		  * @param coll A collection
		  * @return The most extreme item from that collection on this side. None if the collection is empty.
		  */
		def optionFrom(coll: IterableOnce[A]): Option[A] = Extreme.this.optionFrom(coll)(ord)
	}
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
		override def unary_- = Max
		override def compareTo(o: Extreme) = if (o == this) 0 else -1
		
		override def ascendingToExtreme[A](ascendingOrder: Ordering[A]): Ordering[A] = ascendingOrder.reverse
		
		def apply[A](first: A, second: A)(implicit ord: Ordering[A]): A = ord.min(first, second)
		override def from[A](iter: Iterator[A])(implicit ord: Ordering[A]) = iter.min
		override def from[A](coll: Iterable[A])(implicit ord: Ordering[A]) = coll.min
	}
	/**
	  * The maximum / the high extreme
	  */
	case object Max extends Extreme
	{
		override def unary_- = Min
		override def compareTo(o: Extreme) = if (o == this) 0 else 1
		
		override def ascendingToExtreme[A](ascendingOrder: Ordering[A]): Ordering[A] = ascendingOrder
		
		def apply[A](first: A, second: A)(implicit ord: Ordering[A]): A = ord.max(first, second)
		override def from[A](iter: Iterator[A])(implicit ord: Ordering[A]) = iter.max
		override def from[A](coll: Iterable[A])(implicit ord: Ordering[A]) = coll.max
	}
}
