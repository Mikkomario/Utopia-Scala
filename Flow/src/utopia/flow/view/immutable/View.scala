package utopia.flow.view.immutable

import utopia.flow.collection.mutable.iterator.PollableOnce

object View
{
	// OTHER	-----------------------------
	
	/**
	  * @param getValue A function for retrieving the viewed value
	  * @tparam A Type of accessed value
	  * @return A viewable instance that uses the specified function
	  */
	def apply[A](getValue: => A): View[A] = new ViewWrapper[A](getValue)
	
	
	// NESTED	-----------------------------
	
	private class ViewWrapper[+A](_value: => A) extends View[A] {
		override def value = _value
	}
}

/**
  * A common trait for value wrappers that allow others to access (but not necessarily mutate) the underlying value
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  * @tparam A Type of value viewable though this wrapper
  */
trait View[+A] extends Any
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The wrapped value
	  */
	def value: A
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return An iterator of length 1 that returns the value of this view
	  */
	def valueIterator: Iterator[A] = PollableOnce(value)
}
