package utopia.flow.collection.mutable.iterator

/**
 * An iterator that skips one index
 * @author Mikko Hilpinen
 * @since 08.05.2026, v2.9
 */
class WithoutIndexIterator[+A](source: Iterator[A], skippedIndex: Int) extends Iterator[A]
{
	// ATTRIBUTES   ----------------------
	
	private var hasSkipped = skippedIndex < 0
	private var untilSkip = skippedIndex
	
	
	// IMPLEMENTED  ----------------------
	
	override def hasNext: Boolean = {
		// Case: Not skipping next => Checks whether the source has more items
		if (hasSkipped || untilSkip > 0)
			source.hasNext
		// Case: About to skip => Skips the item and checks for the next item
		else {
			source.next()
			hasSkipped = true
			source.hasNext
		}
	}
	
	override def next(): A = {
		// Case: Already skipped => Delegates
		if (hasSkipped)
			source.next()
		// Case: Counting until skip => Adjusts the counter
		else if (untilSkip > 0) {
			untilSkip -= 1
			source.next()
		}
		// Case: Skips => Advances 2 items
		else {
			source.next()
			hasSkipped = true
			source.next()
		}
	}
}
