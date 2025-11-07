package utopia.flow.collection.mutable.iterator

import utopia.flow.view.immutable.caching.Lazy

object AppendToLengthIterator
{
	/**
	 * Creates a new iterator that appends / pads the original collection to a certain length, if possible
	 * @param targetLength Length to which 'primary' is padded, if possible
	 * @param primary The iterator / source to fully iterate first
	 * @param appendFrom The source from which items will be added, unless 'targetLength' has been reached.
	 *                   Call-by-name: Only called once/if appending is necessary.
	 * @tparam A Type of the iterated items
	 * @return A new iterator that iterates 'primary' first, and then appends using 'appendFrom', if necessary
	 */
	def apply[A](targetLength: Int, primary: IterableOnce[A], appendFrom: => IterableOnce[A]) = {
		if (targetLength <= 0 || primary.knownSize >= targetLength)
			primary.iterator
		else
			new AppendToLengthIterator[A](targetLength, primary.iterator, appendFrom.iterator)
	}
}

/**
 * An iterator that appends another iterator at least to length n, taking items from another iterator, if necessary.
 * @param targetLength Length to which the primary source is padded using the appending source
 * @param primarySource The iterator that is iterated first
 * @param getAppendSource A function that yields the iterator to use after the primary source has depleted,
 *                        provided 'minLength' has not been achieved yet.
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.8
 */
class AppendToLengthIterator[A](targetLength: Int, primarySource: Iterator[A], getAppendSource: => Iterator[A])
	extends Iterator[A]
{
	// ATTRIBUTES   ----------------------
	
	/**
	 * A lazy container initialized once/if the appending process starts
	 */
	private val lazyAppendSource = Lazy(getAppendSource)
	
	/**
	 * Counts the number of iterated items, up to [[targetLength]]
	 */
	private var lengthCounter = 0
	/**
	 * Set to true once [[targetLength]] items have been iterated
	 */
	private var hasReachedTargetLength = targetLength <= 0
	
	
	// IMPLEMENTED  ---------------------
	
	override def hasNext: Boolean = {
		// Case: Already of target length => Only the primary source will be iterated
		if (hasReachedTargetLength)
			primarySource.hasNext
		else
			lazyAppendSource.current match {
				// Case: Currently appending => An appending item may be taken, if available
				case Some(source) => source.hasNext
				// Case: Still iterating the main source
				//       => Either that source or the appending source may have the next item
				case None => primarySource.hasNext || lazyAppendSource.value.hasNext
			}
	}
	
	override def knownSize: Int = {
		// Case: Target length reached => Only the primary source will be iterated
		if (hasReachedTargetLength)
			primarySource.knownSize
		else
			lazyAppendSource.current match {
				// Case: Appending => Looks up appending size, and limits it to the specified minimum length
				case Some(appending) => appending.knownSize min (targetLength - lengthCounter)
				// Case: Still iterating the source => Checks whether total size may be determined
				case None =>
					val primarySize = primarySource.knownSize
					if (primarySize < 0 || primarySize + lengthCounter < targetLength)
						-1
					else
						primarySize
			}
	}
	
	override def next(): A = {
		// Case: Only iterating the primary source
		if (hasReachedTargetLength)
			primarySource.next()
		else {
			val result = lazyAppendSource.current match {
				// Case: Appending
				case Some(source) => source.next()
				// Case: Still iterating the primary source => May also take the value from the appending iterator
				case None => primarySource.nextOption().getOrElse { lazyAppendSource.value.next() }
			}
			// Updates the state
			lengthCounter += 1
			if (lengthCounter == targetLength)
				hasReachedTargetLength = true
			result
		}
	}
	
	override def take(n: Int): Iterator[A] = {
		if (hasReachedTargetLength)
			primarySource.take(n)
		else
			super.take(n)
	}
}
