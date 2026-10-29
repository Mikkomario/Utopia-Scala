package utopia.flow.collection.mutable.iterator

object ReducingIterator
{
	/**
	 * @param source Collection to reduce / iterate
	 * @param reduce A function that merges a new item with the existing merge result
	 * @tparam A Type of the iterated items
	 * @return An iterator that starts with the first item in 'source' and then yields reduce results.
	 */
	def apply[A](source: IterableOnce[A])(reduce: (A, A) => A) = new ReducingIterator(source.iterator)(reduce)
}

/**
 * An iterator that continually reduces elements from a source collection
 * @author Mikko Hilpinen
 * @since 28.10.2026, v0.1
 */
class ReducingIterator[A](source: Iterator[A])(reduce: (A, A) => A) extends Iterator[A]
{
	// ATTRIBUTES   -----------------------
	
	private var last: Option[A] = None
	
	
	// IMPLEMENTED  -----------------------
	
	override def hasNext: Boolean = source.hasNext
	override def knownSize = source.knownSize
	
	override def next(): A = {
		val _next = last match {
			case Some(last) => reduce(last, source.next())
			case None => source.next()
		}
		last = Some(_next)
		_next
	}
}
