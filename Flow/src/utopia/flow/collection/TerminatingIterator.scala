package utopia.flow.collection

object TerminatingIterator
{
	/**
	  * @param source Source data iterator (wrapped)
	  * @param terminator A function for testing whether iterating should be terminated
	  * @tparam A Type of iterated item
	  * @return A terminating copy of the specified iterator
	  */
	def apply[A](source: Iterator[A])(terminator: A => Boolean) = new TerminatingIterator[A](source)(terminator)
}

/**
  * An iterator that terminates after a specified condition has been met
  * @author Mikko Hilpinen
  * @since 11.9.2021, v1.11.2
  */
class TerminatingIterator[+A](source: Iterator[A])(terminator: A => Boolean) extends Iterator[A]
{
	// ATTRIBUTES   ----------------------------
	
	private var terminated = false
	
	
	// IMPLEMENTED  ----------------------------
	
	override def hasNext = !terminated && source.hasNext
	
	override def next() =
	{
		if (terminated)
			throw new NoSuchElementException("This iterator has already terminated")
		else
		{
			val nextItem = source.next()
			if (terminator(nextItem))
				terminated = true
			nextItem
		}
	}
}
