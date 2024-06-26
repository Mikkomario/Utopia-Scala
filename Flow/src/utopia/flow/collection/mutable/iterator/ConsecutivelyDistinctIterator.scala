package utopia.flow.collection.mutable.iterator

import scala.annotation.unchecked.uncheckedVariance

object ConsecutivelyDistinctIterator
{
	/**
	  * @param source An iterator to wrap
	  * @tparam A Type of the iterated values
	  * @return A new iterator which skips original iterator values where they are consecutively identical.
	  *         E.g. if the source iterator returns [1, 1, 2, 2, 3, 3, 1, 1], this iterator returns [1, 2, 3, 1].
	  */
	def apply[A](source: Iterator[A]) = new ConsecutivelyDistinctIterator[A](PollingIterator.from(source))
}

/**
  * This iterator ensures that the consecutively returned values are always different
  * @author Mikko Hilpinen
  * @since 24.06.2024, v2.4
  */
class ConsecutivelyDistinctIterator[+A](source: PollingIterator[A]) extends Iterator[A]
{
	// ATTRIBUTES   -------------------------
	
	// unchecked variance because only values from 'source' (which is covariant) will be allocated here
	private var lastValueReturned: Option[A @uncheckedVariance] = None
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasNext: Boolean = lastValueReturned match {
		case Some(v) => source.pollToNextWhere { _ != v }.isDefined
		case None => source.hasNext
	}
	
	override def next(): A = {
		val result = lastValueReturned match {
			case Some(lastValue) =>
				var testedValue = source.next()
				while (testedValue == lastValue)
					testedValue = source.next()
				testedValue
			
			case None => source.next()
		}
		lastValueReturned = Some(result)
		result
	}
}
