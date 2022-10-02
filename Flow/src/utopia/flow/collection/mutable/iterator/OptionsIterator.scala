package utopia.flow.collection.mutable.iterator

import utopia.flow.collection.CollectionExtensions._

object OptionsIterator
{
	/**
	  * Creates a new option-based iterator using an iterate function
	  * @param start The first value (which may be None)
	  * @param iterate An iteration function that takes the latest value and returns a new value or
	  *                None if there are no more values
	  * @tparam A Type of iterated items
	  * @return A new iterator
	  */
	def iterate[A](start: Option[A])(iterate: A => Option[A]) = new OptionsIterator[A](start)(iterate)
	
	/**
	  * Creates a new iterator that generates items until None is encountered
	  * @param generate A function that possibly generates an item, or generates None to terminate the iterator
	  * @tparam A Type of generated items (when present)
	  * @return A new iterator that returns all defined generated items
	  */
	def continually[A](generate: => Option[A]) =
		Iterator.continually(generate).takeWhile { _.isDefined }.flatten
}

/**
  * An iterator that iterates optional values as long as they're available
  * @author Mikko Hilpinen
  * @since 20.8.2022, v1.17
  */
class OptionsIterator[+A](start: Option[A])(iterate: A => Option[A]) extends PollingIterator[A]
{
	// ATTRIBUTES   ------------------
	
	private val source = Iterator.iterate(start) { _.flatMap(iterate)  }.pollable
	
	
	// IMPLEMENTED  ------------------
	
	override def poll = source.poll.get
	override def pollOption = source.pollOption.flatten
	
	override def hasNext = source.pollOption.exists { _.isDefined }
	
	override def next() = source.next().get
	override def skipPolled() = source.skipPolled()
}
