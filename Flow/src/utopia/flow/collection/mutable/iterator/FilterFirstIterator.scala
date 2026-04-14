package utopia.flow.collection.mutable.iterator

import scala.annotation.unchecked.uncheckedVariance

object FilterFirstIterator
{
	/**
	 * @param source Source iterator
	 * @param exclude A function which yields true if the item should be excluded from this iterator
	 * @tparam A Type of the iterated items
	 * @return A new iterator that skips the first item for which 'exclude' yields true
	 */
	def apply[A](source: Iterator[A])(exclude: A => Boolean) = new FilterFirstIterator[A](source)(exclude)
}

/**
 * An iterator that excludes up to one item, based on a filter function
 * @author Mikko Hilpinen
 * @since 12.04.2026, v2.8.1
 */
class FilterFirstIterator[+A](source: Iterator[A])(shouldExclude: A => Boolean) extends Iterator[A]
{
	// ATTRIBUTES   ---------------------
	
	private var excluded = false
	private var polled: Option[A @uncheckedVariance] = None
	
	
	// IMPLEMENTED  ---------------------
	
	override def hasNext: Boolean = {
		// Case: Already polled => Has next
		if (polled.isDefined)
			true
		// Case: Already excluded => Refers to the source
		else if (excluded)
			source.hasNext
		// Case: Not excluded yet => Checks whether the next (source) item should be excluded
		else
			source.nextOption() match {
				case Some(next) =>
					// Case: Excludes the next item => Remembers and refers to the source from now on
					if (shouldExclude(next)) {
						excluded = true
						source.hasNext
					}
					// Case: Not excluded => Prepares it for next()
					else {
						polled = Some(next)
						true
					}
					
				// Case: No more source items => No next
				case None => false
			}
	}
	
	override def next(): A = polled match {
		// Case: Item already polled => Pops the polled item
		case Some(polled) =>
			this.polled = None
			polled
			
		// Case: Not polled
		case None =>
			// Case: Already excluded => Delegates to the source
			if (excluded)
				source.next()
			// Case: Not yet excluded (edge-case) => Checks whether to exclude the next item (may throw)
			else {
				val next = source.next()
				// Case: Should exclude => Skips this item and acquires the next one directly (may throw)
				if (shouldExclude(next)) {
					excluded = true
					source.next()
				}
				// Case: Next item is OK => Yields it
				else
					next
			}
	}
}
