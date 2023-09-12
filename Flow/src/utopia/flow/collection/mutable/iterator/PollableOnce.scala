package utopia.flow.collection.mutable.iterator

import utopia.flow.collection.mutable.iterator.PollableOnce.AlreadyConsumedException

object PollableOnce
{
	// OTHER    --------------------------
	
	/**
	  * Creates a new pollable item
	  * @param value Wrapped value
	  * @tparam A Type of wrapped value
	  * @return A container which returns the value exactly once
	  */
	def apply[A](value: => A) = new PollableOnce[A](value)
	
	
	// NESTED   --------------------------
	
	private class AlreadyConsumedException extends Exception("This container has already been polled / consumed")
}

/**
  * Contains a value which can be retrieved once only, after which it becomes unavailable. Kind of like a reverse Lazy.
  * @author Mikko Hilpinen
  * @since 11.9.2021, v1.11.2
  */
class PollableOnce[+A](value: => A) extends Iterator[A]
{
	// ATTRIBUTES   --------------------------
	
	private var _isConsumed = false
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this container has already been consumed
	  */
	def isConsumed = _isConsumed
	/**
	  * @return Whether this container hasn't yet been consumed
	  */
	def nonConsumed = !isConsumed
	
	
	// IMPLEMENTED  --------------------------
	
	override def hasNext = !isConsumed
	
	override def next() = get()
	
	
	// OTHER    ------------------------------
	
	/**
	  * Consumes the item within this container
	  * @return The consumed item
	  * @throws AlreadyConsumedException If this container was already consumed
	  */
	def get() = if (isConsumed) throw new AlreadyConsumedException() else {
		_isConsumed = true
		value
	}
	
	/**
	  * Consumes the item within this container, if not already
	  * @return The consumed item. None if this container had already been consumed.
	  */
	def poll() = if (isConsumed) None else {
		_isConsumed = true
		Some(value)
	}
}
