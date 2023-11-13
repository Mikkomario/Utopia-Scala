package utopia.flow.operator.enumeration

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End.EndingSequence

/**
  * An enumeration for binary ends, i.e. start & end / first & last
  * @author Mikko Hilpinen
  * @since 1.2.2023, v2.0
  */
sealed trait End extends Binary[End]
{
	// ABSTRACT -----------------------
	
	/**
	  * @param coll A collection of items
	  * @tparam A Type of items in that collection
	  * @return The item at this end of the specified collection.
	  * @throws NoSuchElementException If the specified collection is empty
	  */
	@throws[NoSuchElementException]("If the specified collection is empty")
	def from[A](coll: Iterable[A]): A
	
	/**
	  * @param coll A collection
	  * @return The index at this end of the specified collection.
	  *         NB: The returned value will be out of bounds for empty collections.
	  */
	def indexFrom(coll: Seq[_]): Int
	
	
	// IMPLEMENTED  -------------------
	
	override def self: End = this
	
	
	// OTHER    -----------------------
	
	/**
	  * @param length Targeted number of items
	  * @return A sequence which combines this end and the specified length
	  */
	def apply(length: Int) = EndingSequence(this, length)
	
	/**
	  * @param coll A collection of items
	  * @tparam A Type of items in that collection
	  * @return The item at this end of the specified collection. None if the collection is empty.
	  */
	def optionFrom[A](coll: Iterable[A]) = if (coll.isEmpty) None else Some(from(coll))
	/**
	  * @param coll A collection
	  * @return The index at this end of the specified collection. None if the collection is empty.
	  */
	def optionIndexFrom(coll: Seq[_]) = if (coll.isEmpty) None else Some(indexFrom(coll))
}

object End
{
	// ATTRIBUTES   ------------------
	
	/**
	  * The minimum and the maximum end as a Pair
	  */
	val values = Pair[End](First, Last)
	
	
	// VALUES   ----------------------
	
	/**
	  * The minimum, or the starting end
	  */
	case object First extends End
	{
		override def unary_- = Last
		
		override def compareTo(o: End) = if (o == this) 0 else -1
		
		override def from[A](coll: Iterable[A]): A = coll.head
		override def indexFrom(coll: Seq[_]) = 0
	}
	/**
	  * The maximum, or the completion end
	  */
	case object Last extends End
	{
		override def unary_- = First
		
		override def compareTo(o: End) = if (o == this) 0 else 1
		
		override def from[A](coll: Iterable[A]): A = coll.last
		override def indexFrom(coll: Seq[_]) = coll.size - 1
	}
	
	
	// NESTED   -------------------------
	
	/**
	  * Represents a sequence of n items from one end of a collection
	  * @param end The targeted end
	  * @param length The number of items included
	  */
	case class EndingSequence(end: End, length: Int)
}