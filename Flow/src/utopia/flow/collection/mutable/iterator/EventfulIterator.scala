package utopia.flow.collection.mutable.iterator

import utopia.flow.event.model.ChangeEvent
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.LockablePointer
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

object EventfulIterator
{
	/**
	  * Creates a new eventful iterator
	  * @param initialValue Value to hold initially
	  * @param source An iterator that acts as the source of new values
	  * @tparam A Type of values returned by this iterator
	  * @return A new eventful iterator
	  */
	def apply[A](initialValue: A, source: Iterator[A])(implicit log: Logger) =
		new EventfulIterator[A](initialValue, source)
}

/**
  * Wraps an iterator, adding support for change events
  * @author Mikko Hilpinen
  * @since 22.9.2022, v1.17
  */
class EventfulIterator[A](initialValue: A, source: Iterator[A])(implicit log: Logger)
	extends Iterator[A] with ChangingWrapper[A]
{
	// ATTRIBUTES   -------------------
	
	private val pointer = LockablePointer[A](initialValue)
	
	
	// IMPLEMENTED  -------------------
	
	override def listenerLogger: Logger = log
	
	override def hasNext = {
		val result = source.hasNext
		// Once there are no more items available, marks the pointer as locked
		if (!result)
			pointer.lock()
		result
	}
	
	override def next() = {
		val n = source.next()
		pointer.value = n
		n
	}
	
	override protected def wrapped: Changing[A] = pointer
	
	override def map[B](f: A => B): Iterator[B] with Changing[B] = new MappingIteratorWithEvents[A, B](this)(f)
	
	
	// OTHER    ---------------------
	
	/**
	  * Moves this iterator forward one step (exactly like next()),
	  * but returns a change event instead of just the new value
	  * @return Change event from this iterator's last state to the new
	  */
	def nextEvent() = {
		val oldValue = value
		val newValue = next()
		ChangeEvent(oldValue, newValue)
	}
	
	
	// NESTED   --------------------
	
	private class MappingIteratorWithEvents[O, R](source: Iterator[O] with Changing[O])(f: O => R)
		extends ChangingWrapper[R] with Iterator[R]
	{
		// ATTRIBUTES   ------------
		
		private val pointer = LockablePointer[R](f(source.value))(log)
		
		
		// INITIAL CODE ------------
		
		source.addListener { e => pointer.value = f(e.newValue) }
		
		
		// IMPLEMENTED  ------------
		
		override implicit def listenerLogger: Logger = source.listenerLogger
		override protected def wrapped = pointer
		
		override def hasNext = {
			val result = source.hasNext
			if (!result)
				pointer.lock()
			result
		}
		
		override def next() = {
			val n = f(source.next())
			pointer.value = n
			n
		}
		
		override def map[B](f: R => B): Iterator[B] with Changing[B] =
			new MappingIteratorWithEvents[R, B](this)(f)
	}
}
