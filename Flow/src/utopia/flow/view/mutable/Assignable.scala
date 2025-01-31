package utopia.flow.view.mutable

import scala.language.implicitConversions

object Assignable
{
	// IMPLICIT ---------------------
	
	/**
	  * Implicitly wraps pointers as assignable instances
	  * @param pointer A pointer to represent as an Assignable instance
	  * @tparam A Type of pointer values
	  * @return An Assignable wrapping the specified pointer
	  */
	implicit def wrap[A](pointer: Pointer[A]): Assignable[A] = new PointerIsAssignable[A](pointer)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param set A set function to wrap
	  * @tparam A Type of values accepted by the function
	  * @return A new assignable that wraps the specified function
	  */
	def apply[A](set: A => Unit): Assignable[A] = new AssignableFunction[A](set)
	
	
	// NESTED   ---------------------
	
	private class AssignableFunction[-A](f: A => Unit) extends Assignable[A]
	{
		override def set(value: A): Unit = f(value)
	}
	
	private class PointerIsAssignable[A](pointer: Pointer[A]) extends Assignable[A]
	{
		override def set(value: A): Unit = pointer.value = value
	}
}

/**
  * Common trait for interfaces which may be given a value, exposing a setter function
  * @tparam A Type of values that may be assigned to this interface
  * @author Mikko Hilpinen
  * @since 31.01.2025, v2.6
  */
trait Assignable[-A]
{
	/**
	  * Changes the value of this item
	  * @param value New value to assign
	  */
	def set(value: A): Unit
}
