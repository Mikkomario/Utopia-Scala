package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.template.Viewable

/**
  * A common trait for wrapper classes which allow direct mutation and replacement of the wrapped value
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
trait Settable[A] extends Viewable[A]
{
	// ABSTRACT	----------------------------
	
	/**
	  * Updates the value in this wrapper
	  * @param newValue The new value to assign to this wrapper
	  */
	def value_=(newValue: A): Unit
	
	
	// OTHER	----------------------------
	
	/**
	  * Mutates or modifies the value in this wrapper
	  * @param f A mapping / modifying function applied to the current value of this wrapper. The value returned by
	  *          this function is assigned to this wrapper.
	  */
	def update(f: A => A) = value = f(value)
}
