package utopia.flow.collection.mutable.iterator

/**
  * An iterator wrapper that will only return up to a certain number of items
  * @author Mikko Hilpinen
  * @since 11.9.2021, v1.12
  */
class LimitedLengthIterator[+A](source: Iterator[A], val maxLength: Int) extends Iterator[A]
{
	// ATTRIBUTES   ---------------------------
	
	private var consumeCount = 0
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return Whether this iterator has reached the maximum allowed length
	  */
	def isConsumed = consumeCount >= maxLength
	/**
	  * @return Whether this iterator has not reached the maximum allowed length
	  */
	def nonConsumed = !isConsumed
	
	/**
	  * @return Maximum number of remaining number of items that can be returned
	  */
	def remainingMaxLength = maxLength - consumeCount
	
	
	// IMPLEMENTED  ---------------------------
	
	override def hasNext = nonConsumed && source.hasNext
	
	override def next() = {
		if (isConsumed)
			throw new NoSuchElementException(
				s"Maximum number ($maxLength) of items have already been taken from this iterator")
		else {
			consumeCount += 1
			source.next()
		}
	}
}
