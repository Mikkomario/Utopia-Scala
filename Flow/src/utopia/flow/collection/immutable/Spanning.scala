package utopia.flow.collection.immutable

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @tparam P Type of end-points in this span
  * @tparam D Type of distance / step used by this span
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait Spanning[P, D] extends HasInclusiveEnds[P] with RangeLike[P, D]
{
	// COMPUTED -----------------------
	
	/**
	  * @return An iterator that starts from the end of this span and moves towards the start
	  */
	def reverseIterator = reverseIteratorBy(step)
	
	
	// IMPLEMENTED  --------------------
	
	override def head = start
	override def headOption = Some(start)
	
	override def isEmpty = false
	
	override def min[B >: P](implicit ord: Ordering[B]) = super[HasInclusiveEnds].min
	override def minOption[B >: P](implicit ord: Ordering[B]) = Some(min)
	
	override def max[B >: P](implicit ord: Ordering[B]) = super[HasInclusiveEnds].max
	override def maxOption[B >: P](implicit ord: Ordering[B]) = Some(max)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param step A step taken each iteration
	  * @return An iterator that starts from the end of this span and moves towards the start
	  */
	def reverseIteratorBy(step: D) = _iterator(end, start, step)
}
