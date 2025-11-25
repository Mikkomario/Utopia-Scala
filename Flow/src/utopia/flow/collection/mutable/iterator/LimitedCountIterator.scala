package utopia.flow.collection.mutable.iterator

object LimitedCountIterator
{
	/**
	 * @param source Source from which items are collected
	 * @param targetCount Targeted number of items
	 * @param f A function that yields true for targeted items
	 * @tparam A Type of the iterated items
	 * @return An iterator that only collects up to 'targetCount' targeted items before terminating
	 */
	def apply[A](source: IterableOnce[A], targetCount: Int)(f: A => Boolean) =
		new LimitedCountIterator[A](source.iterator, targetCount)(f)
}

/**
 * An iterator that only collects up to a certain number of target elements before terminating
 * @author Mikko Hilpinen
 * @since 25.11.2025, v2.8
 */
class LimitedCountIterator[+A](source: Iterator[A], targetCount: Int)(f: A => Boolean) extends Iterator[A]
{
	// ATTRIBUTES   ---------------------
	
	private var foundCount = 0
	
	
	// IMPLEMENTED  ---------------------
	
	override def hasNext: Boolean = foundCount < targetCount && source.hasNext
	
	override def next(): A = {
		val result = source.next()
		if (f(result))
			foundCount += 1
		result
	}
}
