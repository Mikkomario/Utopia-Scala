package utopia.flow.collection

import utopia.flow.datastructure.mutable.{PollableOnce, ResettableLazy}

import scala.collection.immutable.VectorBuilder

/**
 * An iterator (wrapper) which enables one to poll the next item before consuming it
 * @author Mikko Hilpinen
 * @since 5.4.2021, v1.9
 */
class PollingIterator[A](source: Iterator[A]) extends Iterator[A]
{
	// ATTRIBUTES   -------------------------
	
	private val pollCache = ResettableLazy { source.next() }
	
	
	// COMPUTED -----------------------------
	
	/**
	 * Checks the next available item in this iterator without consuming it
	 * (Meaning: The next call of next() or poll() will still yield that same item)
	 * @return The next item in this iterator
	 * @throws NoSuchElementException If there are no more elements in this iterator
	 */
	@throws[NoSuchElementException]("If there are no more elements in this iterator")
	def poll = pollCache.value
	
	/**
	 * Checks the next available item in this iterator without consuming it
	 * (Meaning: The next call of next() or poll() will still yield that same item)
	 * @return The next item in this iterator. None if there are no more items available.
	 */
	def pollOption = if (hasNext) Some(poll) else None
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasNext = pollCache.isInitialized || source.hasNext
	
	override def next() = pollCache.popCurrent().getOrElse { source.next() }
	
	override def map[B](f: A => B) = pollCache.popCurrent() match {
		case Some(polled) => PollableOnce(polled).map(f) ++ source.map(f)
		case None => source.map(f)
	}
	override def flatMap[B](f: A => IterableOnce[B]) = pollCache.popCurrent() match
	{
		case Some(polled) => PollableOnce(polled).flatMap(f) ++ source.flatMap(f)
		case None => source.flatMap(f)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * Skips the polled item, if there was one
	  */
	def skipPolled() = pollCache.reset()
	
	/**
	 * Takes the next item if a) there is one available and b) it fulfills the specified condition.
	 * If the item was not accepted, this iterator is not advanced.
	 * @param condition Condition that must be fulfilled for the item to be taken and this iterator to be advanced
	 * @return The next item, if one is available and it fills the condition. None otherwise.
	 */
	def nextIf(condition: A => Boolean) = if (pollOption.exists(condition)) Some(next()) else None
	
	/**
	  * Advances this iterator until the next item satisfies the specified condition or the end of this iterator is met.
	  * @param condition A search condition
	  * @return The next item that will satisfy the specified condition. This will also be returned by .pollOption
	  *         and .nextOption()
	  */
	def pollToNextWhere(condition: A => Boolean) =
	{
		while (pollOption.exists { item => !condition(item) })
		{
			next()
		}
		pollOption
	}
	
	/**
	 * Performs the specified action to each item in this iterator while the specified condition is met.
	 * After this method call, the next item in this iterator, if there is one, will be the item that
	 * didn't fulfill that condition.
	 * @param condition A condition for continuing operations
	 * @param f Function performed to items that fulfilled that condition
	 */
	def foreachWhile(condition: A => Boolean)(f: A => Unit) =
	{
		while (pollOption.exists(condition))
		{
			f(next())
		}
	}
	/**
	 * Performs the specified action to each item in this iterator until the specified condition is met.
	 * After this method call, the next item in this iterator, if there is one, will be the item that
	 * fulfilled that condition.
	 * @param terminator A condition for stopping the iteration
	 * @param f Function performed to items that fulfilled that condition
	 */
	def foreachUntil(terminator: A => Boolean)(f: A => Unit) = foreachWhile { !terminator(_) }(f)
	
	/**
	 * Takes items as long as they fulfill the specified condition. After this method call, the next item will
	 * <b>not</b> fulfill the specified condition, or there won't be any items left.
	 * @param condition Condition that must be fulfilled for an item to be included
	 * @return All of the consecutive items which fulfilled the specified condition
	 */
	def collectWhile(condition: A => Boolean) =
	{
		val resultsBuilder = new VectorBuilder[A]()
		foreachWhile(condition) { resultsBuilder += _ }
		resultsBuilder.result()
	}
	/**
	 * Takes items as long as they don't fulfill the specified condition. After this method call, the next item will
	 * fulfill the specified condition, or there won't be any items left.
	 * @param terminator Condition that returns true for the first excluded item
	 * @return All of the consecutive items which didn't fulfill the specified condition
	 */
	def collectUntil(terminator: A => Boolean) = collectWhile { !terminator(_) }
	/**
	 * Takes items as long as they fulfill the specified condition. After this method call, the next item will
	 * <b>not</b> fulfill the specified condition, or there won't be any items left.
	 * @param condition Condition that must be fulfilled for an item to be included
	 * @return All of the consecutive items which fulfilled the specified condition
	 */
	@deprecated("Please use .collectWhile(...) instead - take-methods are usually meant to return iterators", "v1.12.1")
	def takeNextWhile(condition: A => Boolean) = collectWhile(condition)
}
