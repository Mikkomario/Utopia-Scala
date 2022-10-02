package utopia.flow.view.mutable.caching

import utopia.flow.view.mutable.Pointer

object MutableLazy
{
	// OTHER    ---------------------
	
	/**
	  * Creates a new lazy
	  * @param make A function that makes the value (call by name)
	  * @tparam A The type of cached item
	  * @return A new lazy container
	  */
	def apply[A](make: => A): MutableLazy[A] = new _MutableLazy(make)
	
	
	// NESTED   ---------------------
	
	private class _MutableLazy[A](generator: => A) extends MutableLazy[A]
	{
		// ATTRIBUTES    ----------------
		
		private var _value: Option[A] = None
		
		
		// IMPLEMENTED    ---------------
		
		override def value_=(newValue: A) = _value = Some(newValue)
		
		override def reset() = {
			if (_value.isDefined) {
				_value = None
				true
			}
			else
				false
		}
		
		override def value = current match {
			case Some(value) => value
			case None =>
				val newValue = generator
				_value = Some(newValue)
				newValue
		}
		
		override def current = _value
	}
}

/**
  * A common trait for lazy container implementations which allow outside manipulation
  * @author Mikko Hilpinen
  * @since 22.7.2020, v1.8
  */
trait MutableLazy[A] extends ResettableLazy[A] with Pointer[A]
