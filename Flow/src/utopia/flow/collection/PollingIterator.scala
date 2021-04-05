package utopia.flow.collection

import scala.collection.immutable.VectorBuilder

/**
 * An iterator (wrapper) which enables one to poll the next item before consuming it
 * @author Mikko Hilpinen
 * @since 5.4.2021, v1.9
 */
class PollingIterator[A](source: Iterator[A]) extends Iterator[A]
{
	// ATTRIBUTES   -------------------------
	
	private var polled: Option[A] = None
	
	
	// COMPUTED -----------------------------
	
	/**
	 * Checks the next available item in this iterator without consuming it
	 * (Meaning: The next call of next() or poll() will still yield that same item)
	 * @return The next item in this iterator
	 * @throws NoSuchElementException If there are no more elements in this iterator
	 */
	@throws[NoSuchElementException]("If there are no more elements in this iterator")
	def poll = polled match
	{
		case Some(item) => item
		// Polls a new item if necessary
		case None =>
			val item = source.next()
			polled = Some(item)
			item
	}
	
	/**
	 * Checks the next available item in this iterator without consuming it
	 * (Meaning: The next call of next() or poll() will still yield that same item)
	 * @return The next item in this iterator. None if there are no more items available.
	 */
	def pollOption = if (hasNext) Some(poll) else None
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasNext = polled.nonEmpty || source.hasNext
	
	override def next() = polled match
	{
		// Consumes the polled item first, if there is one
		case Some(item) =>
			polled = None
			item
		case None => source.next()
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * Takes the next item if a) there is one available and b) it fulfills the specified condition.
	 * If the item was not accepted, this iterator is not advanced.
	 * @param condition Condition that must be fulfilled for the item to be taken and this iterator to be advanced
	 * @return The next item, if one is available and it fills the condition. None otherwise.
	 */
	def nextIf(condition: A => Boolean) = if (pollOption.exists(condition)) Some(next()) else None
	
	/**
	 * Takes items as long as they fulfill the specified condition. After this method call, the next item will
	 * <b>not</b> fulfill the specified condition, or there won't be any items left.
	 * @param condition Condition that must be fulfilled for an item to be included
	 * @return All of the consecutive items which fulfilled the specified condition
	 */
	def takeNextWhile(condition: A => Boolean) =
	{
		val resultsBuilder = new VectorBuilder[A]()
		while (pollOption.exists(condition))
		{
			resultsBuilder += next()
		}
		resultsBuilder.result()
	}
}
