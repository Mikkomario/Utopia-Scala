package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.template.LazyLike

/**
  * A common trait for lazily initialized containers which allow the wrapped value to be reset, so that it is generated
  * possibly multiple times
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
trait ResettableLazyLike[+A] extends LazyLike[A]
{
	// ABSTRACT	----------------------
	
	/**
	  * Resets this lazy container so that a new value is generated the next time it is requested
	  */
	def reset(): Unit
	
	
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
}
