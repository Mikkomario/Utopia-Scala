package utopia.flow.collection.mutable.iterator

import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.{ChangingLike, ChangingWrapper}

/**
  * Wraps an iterator, adding support for change events
  * @author Mikko Hilpinen
  * @since 22.9.2022, v1.17
  */
class IteratorWithEvents[A](initialValue: A, source: Iterator[A]) extends Iterator[A] with ChangingWrapper[A]
{
	// ATTRIBUTES   -------------------
	
	private val pointer = new PointerWithEvents[A](initialValue)
	
	
	// IMPLEMENTED  -------------------
	
	override def hasNext = source.hasNext
	
	override def next() = {
		val n = source.next()
		pointer.value = n
		n
	}
	
	override protected def wrapped = pointer
	
	override def isChanging = hasNext
	
	override def map[B](f: A => B): Iterator[B] with ChangingLike[B] =
		new MappingIteratorWithEvents[A, B](this)(f)
	
	
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
	
	private class MappingIteratorWithEvents[O, R](source: Iterator[O] with ChangingLike[O])(f: O => R)
		extends ChangingWrapper[R] with Iterator[R]
	{
		// ATTRIBUTES   ------------
		
		private val pointer = new PointerWithEvents[R](f(source.value))
		
		
		// INITIAL CODE ------------
		
		source.addListener { e => pointer.value = f(e.newValue) }
		
		
		// IMPLEMENTED  ------------
		
		override protected def wrapped = pointer
		
		override def isChanging = hasNext
		override def hasNext = source.hasNext
		
		override def next() = {
			val n = f(source.next())
			pointer.value = n
			n
		}
		
		override def map[B](f: R => B): Iterator[B] with ChangingLike[B] =
			new MappingIteratorWithEvents[R, B](this)(f)
	}
}
