package utopia.flow.util

import utopia.flow.operator.ordering.RichComparable

/**
  * An utility item for testing the size of some collection using .sizeCompare(...) instead of .size == ...
  * @author Mikko Hilpinen
  * @since 24.1.2023, v2.0
  */
class HasSize(c: Iterable[_]) extends RichComparable[Int]
{
	// IMPLEMENTED  ------------------------
	
	override def compareTo(o: Int) = c.sizeCompare(o)
	
	// Some operations are optimized in order to avoid extra iterations
	override def <(other: Int) = c.sizeCompare(other - 1) <= 0
	override def >=(other: Int) = c.sizeCompare(other - 1) > 0
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param size A size
	  * @return Whether this collection has exactly that size
	  */
	def apply(size: Int) = c.sizeCompare(size) == 0
	
	/**
	  * @param coll Another collection
	  * @return Whether this collection has the same size as the other collection
	  */
	def of(coll: Iterable[_]): Boolean = c.sizeCompare(coll) == 0
	
	/**
	  * @param coll A collection
	  * @return Whether this collection has the same size as the other collection
	  */
	def ==(coll: Iterable[_]) = of(coll)
	/**
	  * @param coll Another collection
	  * @return Whether this collection has a greater size than the other collection
	  */
	def >(coll: Iterable[_]) = c.sizeCompare(coll) > 0
	/**
	  * @param coll Another collection
	  * @return Whether this collection has a smaller size than the other collection
	  */
	def <(coll: Iterable[_]) = c.sizeCompare(coll) < 0
	/**
	  * @param coll Another collection
	  * @return Whether this collection has a greater or equal size than the other collection
	  */
	def >=(coll: Iterable[_]) = c.sizeCompare(coll) >= 0
	/**
	  * @param coll Another collection
	  * @return Whether this collection has a smaller or equal size than the other collection
	  */
	def <=(coll: Iterable[_]) = c.sizeCompare(coll) <= 0
}
