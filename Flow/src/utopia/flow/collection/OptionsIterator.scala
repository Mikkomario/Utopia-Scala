package utopia.flow.collection

import utopia.flow.util.CollectionExtensions._

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
