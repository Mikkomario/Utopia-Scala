package utopia.flow.view.immutable

import utopia.flow.collection.mutable.iterator.PollableOnce
import utopia.flow.view.immutable.caching.Lazy

object View
{
	// OTHER	-----------------------------
	
	/**
	  * @param getValue A function for retrieving the viewed value
	  * @tparam A Type of accessed value
	  * @return A viewable instance that uses the specified function
	  */
	def apply[A](getValue: => A): View[A] = new ViewWrapper[A](getValue)
	
	/**
	  * @param value A fixed value
	  * @tparam A Type of that value
	  * @return A view wrapping that value
	  */
	def fixed[A](value: A): View[A] = FixedView[A](value)
	
	
	// IMPLICIT -----------------------------
	
	implicit class DeepView[A](val v: View[View[A]]) extends AnyVal
	{
		/**
		  * @return A view into the value of this view's value
		  */
		def flatten = View { v.value.value }
	}
	
	
	// NESTED	-----------------------------
	
	private class ViewWrapper[+A](_value: => A) extends View[A] {
		override def value = _value
	}
	
	private case class FixedView[+A](override val value: A) extends View[A] {
		override def valueIterator = Iterator.single(value)
		override def mapValue[B](f: A => B) = FixedView(f(value))
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
	
	
	// OTHER    ---------------------------
	
	/**
	  * Creates a new view that yields a mapped value of this view
	  * @param f A mapping function to apply
	  * @tparam B Type of mapping result
	  * @return A new view that yields the mapped value / values
	  */
	def mapValue[B](f: A => B): View[B] = MappingView(this)(f)
}
