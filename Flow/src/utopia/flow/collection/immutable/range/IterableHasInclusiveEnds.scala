package utopia.flow.collection.immutable.range

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @tparam P Type of end-points in this span
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait IterableHasInclusiveEnds[P] extends HasInclusiveOrderedEnds[P] with IterableHasEnds[P]
{
	// COMPUTED -----------------------
	
	/**
	  * @return An iterator that starts from the end of this span and moves towards the start
	  */
	def reverseIterator = _iterator(end, start)
	
	
	// IMPLEMENTED  --------------------
	
	override def head = start
	override def headOption = Some(start)
	
	override def isEmpty = false
	
	override def min[B >: P](implicit ord: Ordering[B]) = super[HasInclusiveOrderedEnds].min
	override def minOption[B >: P](implicit ord: Ordering[B]) = Some(min)
	
	override def max[B >: P](implicit ord: Ordering[B]) = super[HasInclusiveOrderedEnds].max
	override def maxOption[B >: P](implicit ord: Ordering[B]) = Some(max)
}
