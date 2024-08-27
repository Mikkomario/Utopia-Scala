package utopia.flow.view.mutable.async

import utopia.flow.collection.immutable.Empty
import utopia.flow.view.mutable.Switch

object VolatileSwitch
{
	// OTHER    -------------------------
	
	/**
	  * @param initialState Initial state to assign to this switch (default = false)
	  * @return A new switch
	  */
	def apply(initialState: Boolean = false): VolatileSwitch = new _VolatileSwitch(initialState)
	
	
	// NESTED   -------------------------
	
	private class _VolatileSwitch(initialState: Boolean) extends VolatileSwitch
	{
		// ATTRIBUTES   -----------
		
		@volatile private var _value: Boolean = initialState
		
		
		// IMPLEMENTED  -----------
		
		override def value: Boolean = _value
		
		override protected def assign(newValue: Boolean): Seq[() => Unit] = {
			_value = newValue
			Empty
		}
	}
}

/**
  * Common trait for thread-safe implementations of the [[Switch]] trait
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
trait VolatileSwitch extends Volatile[Boolean] with Switch