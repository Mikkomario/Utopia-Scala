package utopia.flow.view.mutable

import utopia.flow.view.template.MaybeSet

object Settable
{
	// COMPUTED --------------------------
	
	/**
	  * @return A Settable instance that has already been set (and can't be mutated)
	  */
	def alreadySet: Settable = AlreadySet
	
	
	// OTHER    --------------------------
	
	/**
	  * @return A new Settable instance in false state. May be set once.
	  */
	def apply(): Settable = new _Settable
	
	/**
	  * Creates a new Settable instance with a predefined state
	  * @param initialState Whether this instance has already been set
	  * @return A Settable with the specified initial state
	  */
	def apply(initialState: Boolean): Settable = if (initialState) AlreadySet else apply()
	
	
	// NESTED   --------------------------
	
	private object AlreadySet extends Settable
	{
		override def isSet: Boolean = true
		override def set(): Boolean = false
	}
	
	private class _Settable extends Settable
	{
		// ATTRIBUTES   ------------------
		
		private var state: Boolean = false
		
		
		// IMPLEMENTED  ------------------
		
		override def isSet: Boolean = state
		
		override def set(): Boolean = {
			if (state)
				false
			else {
				state = true
				true
			}
		}
	}
}

/**
  * Common trait for that have a mutable (typically boolean-based) state,
  * which may be altered without providing additional parameters.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
trait Settable extends MaybeSet
{
	// ABSTRACT -------------------------
	
	/**
	  * "Sets" this item, possibly altering its state
	  * @return Whether the state of this item was altered.
	  */
	def set(): Boolean
}
