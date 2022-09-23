package utopia.flow.collection.mutable

import utopia.flow.collection.template.Viewable

object Settable
{
	implicit class OptionPointer[A](val p: Settable[Option[A]]) extends AnyVal
	{
		def value_=(newValue: A) = p.value = Some(newValue)
		
		/**
		  * Removes the current value from this pointer
		  */
		def clear() = p.value = None
		
		/**
		  * @return Removes and returns the current value from this pointer
		  */
		def pop() = p.getAndSet(None)
		
		/**
		  * Updates the value of this pointer if there is no value already, returns the value after the update
		  * @param v Value to assign if there is no value in this pointer (call by name)
		  * @return Existing pointer value or the assigned value
		  */
		def getOrElseUpdate(v: => A) = p.value.getOrElse {
			val newValue = v
			p.value = Some(newValue)
			newValue
		}
	}
	
	implicit class VectorPointer[A](val p: Settable[Vector[A]]) extends AnyVal
	{
		/**
		  * Removes all values from this pointer
		  */
		def clear() = p.value = Vector()
		
		/**
		  * @return Removes and returns the first value in this pointer
		  */
		def pop(): Option[A] = p.pop { v =>
			if (v.isEmpty)
				None -> v
			else
				Some(v.head) -> v.tail
		}
		/**
		  * @return Removes and returns the last value in this pointer
		  */
		def popLast() = p.pop { v => v.lastOption -> v.dropRight(1) }
		
		/**
		  * Clears all items from this pointer
		  * @return Value before this method call
		  */
		def popAll() = p.getAndSet(Vector())
	}
}

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
	
	/**
	  * Updates the contents of this pointer and returns the value after the update
	  * @param f A function for modifying the contents of this pointer
	  * @return Value after the update
	  */
	def updateAndGet(f: A => A) =
	{
		update(f)
		value
	}
	
	/**
	  * Updates the contents of this pointer and returns the value before the update
	  * @param f A function for modifying the contents of this pointer
	  * @return Value before the update
	  */
	def getAndUpdate(f: A => A) =
	{
		val result = value
		value = f(result)
		result
	}
	
	/**
	  * Updates the contents of this pointer. Returns the state before the update.
	  * @param newValue New value to assign to this pointer.
	  * @return Previous value of this pointer.
	  */
	def getAndSet(newValue: A) =
	{
		val result = value
		value = newValue
		result
	}
	
	/**
	  * Mutates the value in this wrapper, also returning an alternative result
	  * @param mutate A mutator function that returns the final result and the new value
	  * @tparam B Type of the additional result
	  * @return The additional result returned by the mutator function
	  */
	private def pop[B](mutate: A => (B, A)) =
	{
		val (result, newValue) = mutate(value)
		value = newValue
		result
	}
}
