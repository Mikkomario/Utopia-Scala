package utopia.flow.collection.immutable

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasInclusiveEnds[P] extends HasEnds[P]
{
	// COMPUTED -----------------------
	
	/**
	  * @return The minimum and the maximum of this span
	  */
	def minMax: Pair[P] = toPair.sorted
	
	/**
	  * @return Whether this span is of length 1 (i.e. the start and the end overlap)
	  */
	def isUnit = start == end
	
	
	// IMPLEMENTED  --------------------
	
	override def isInclusive = true
	
	override def isEmpty = false
	
	def min[B >: P](implicit ord: Ordering[B]) = ord.min(start, end)
	def minOption[B >: P](implicit ord: Ordering[B]) = Some(min)
	
	def max[B >: P](implicit ord: Ordering[B]) = ord.max(start, end)
	def maxOption[B >: P](implicit ord: Ordering[B]) = Some(max)
}
