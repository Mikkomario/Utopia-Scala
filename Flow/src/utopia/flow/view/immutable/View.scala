package utopia.flow.view.immutable

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
	
	private class ViewWrapper[+A](_value: => A) extends View[A]
	{
		override def value = _value
	}
}

/**
  * A common trait for value wrappers that allow others to access (but not necessarily mutate) the underlying value
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  * @tparam A Type of value viewable though this wrapper
  */
trait View[+A]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The wrapped value
	  */
	def value: A
}
