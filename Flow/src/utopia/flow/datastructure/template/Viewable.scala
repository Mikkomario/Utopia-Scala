package utopia.flow.datastructure.template

object Viewable
{
	// OTHER	-----------------------------
	
	/**
	  * @param getValue A function for retrieving the viewed value
	  * @tparam A Type of accessed value
	  * @return A viewable instance that uses the specified function
	  */
	def apply[A](getValue: => A): Viewable[A] = new ViewableWrapper[A](getValue)
	
	
	// NESTED	-----------------------------
	
	private class ViewableWrapper[+A](_value: => A) extends Viewable[A]
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
trait Viewable[+A]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The wrapped value
	  */
	def value: A
	
	
	// OTHER	----------------------------
	
	/* Removed due to name clashes in VolatileOption (and possibly other places)
	/**
	  * @param item An item
	  * @return Whether this wrapper (currently) contains the specified item
	  */
	def contains(item: Any) = value == item
	
	/**
	  * @param condition A condition
	  * @return Whether the specified condition holds true for the wraped item
	  */
	def exists(condition: A => Boolean) = condition(value)
	
	/**
	  * @param f A filtering function
	  * @return Wrapped item if the function holds true for it. None if the function doesn't hold true.
	  */
	def filter(f: A => Boolean) = Some(value).filter(f)
	
	/**
	  * @param f A filtering function
	  * @return None if the function holds true for this wrapper's item, the wrapped item otherwise.
	  */
	def filterNot(f: A => Boolean) = Some(value).filterNot(f)*/
}
