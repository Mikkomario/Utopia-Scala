package utopia.flow.view.mutable.caching

import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Resettable

/**
  * A common trait for lazily initialized containers which allow the wrapped value to be reset, so that it is generated
  * possibly multiple times
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
trait ResettableLazyLike[+A] extends Lazy[A] with Resettable
{
	// OTHER	----------------------
	
	/**
	  * Retrieves a value from this lazy and resets it afterwards
	  * @return The value in this lazy before the reset
	  */
	def pop() =
	{
		val result = value
		reset()
		result
	}
	
	/**
	  * Retrieves a value from this lazy, if one has already been initialized, and resets it afterwards (if necessary)
	  * @return The value in this lazy before the reset. None if this lazy didn't have an initialized value.
	  */
	def popCurrent() =
	{
		val result = current
		if (result.nonEmpty)
			reset()
		result
	}
	
	/**
	  * Resets this lazy instance and immediately requests a new value
	  * @return The newly generated and stored value
	  */
	def newValue() =
	{
		reset()
		value
	}
	
	/**
	  * Keeps the current value only if it fulfills the specified condition
	  * @param keepCondition A condition for the currently cached value to keep
	  */
	def filter(keepCondition: A => Boolean) = filterNot { c => !keepCondition(c) }
	
	/**
	  * Clears the current value if it matches the specified condition
	  * @param resetCondition A condition on which the cached item is cleared
	  */
	def filterNot(resetCondition: A => Boolean) = {
		if (current.exists(resetCondition))
			reset()
	}
}
