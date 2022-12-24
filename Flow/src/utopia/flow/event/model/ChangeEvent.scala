package utopia.flow.event.model

import utopia.flow.collection.immutable.Pair

object ChangeEvent
{
	/**
	  * Creates a new change event
	  * @param oldValue The value before this change occurred
	  * @param newValue The value after this change occurred
	  * @tparam A Type of changed value
	  * @return A new change event
	  */
	def apply[A](oldValue: A, newValue: A): ChangeEvent[A] = apply(Pair(oldValue, newValue))
}

/**
  * Change events are generated when a value changes
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1+
  * @tparam A the type of changed item
  * @param values The old and the new value
  */
case class ChangeEvent[+A](values: Pair[A])
{
	// COMPUTED    ---------------------------------
	
	/**
	  * @return The value before this change occurred
	  */
	def oldValue = values.first
	/**
	  * @return The value after this change occurred
	  */
	def newValue = values.second
	
	/**
	  * @return A pair containing:
	  *         - 1: The old value
	  *         - 2: The new value
	  */
	def toPair = values
	
	
	// IMPLEMENTED	-----------------------------
	
	override def toString = s"Change from $oldValue to $newValue"
	
	
	// OTHER	---------------------------------
	
	/**
	  * Maps this change event with a function that modifies a pair
	  * @param f A function that modifies a pair
	  * @tparam B Type of resulting pair contents
	  * @return A mapped copy of this change event, using the modified value pair
	  */
	def mapAsPair[B](f: Pair[A] => Pair[B]) = ChangeEvent(f(values))
	
	/**
	  * Maps the old and the new value in this event
	  * @param f A mapping function for the old and the new value (called twice)
	  * @tparam B Type of new values
	  * @return A copy of this change event with mapped values
	  */
	def map[B](f: A => B) = mapAsPair { _.map(f) }
	
	/**
	  * Converts this change event to a string using a custom toString for the changed values
	  * @param f A toString function used for the changed values
	  * @return A string based on this change event
	  */
	def toStringWith(f: A => String) = s"Change from ${ f(oldValue) } to ${ f(newValue) }"
	
	/**
	  * Checks whether certain aspects of the old and new value are equal
	  * @param map A mapping function applied for both the old and the new value
	  * @tparam B Type of the mapped value
	  * @return True if the mapped values are equal, false otherwise
	  */
	def equalsBy[B](map: A => B) = map(oldValue) == map(newValue)
	@deprecated("Replaced with .equalsBy(...)")
	def compareBy[B](map: A => B) = equalsBy(map)
	/**
	 * Checks whether certain aspects of the old and new value are different
	 * @param map A mapping function applied for both the old and the new value
	 * @tparam B Type of the mapped value
	 * @return True if the mapped values are different, false otherwise
	 */
	def notEqualsBy[B](map: A => B) = !equalsBy(map)
	@deprecated("Replaced with .notEqualsBy(...)")
	def differentBy[B](map: A => B) = notEqualsBy(map)
	/**
	  * Applies a function from the old value to the new value
	  * @param f A function for mapping into the applied function in the old value
	  * @tparam R Result type of the found function
	  * @return Result value of the found function when applied with the new value
	  */
	@deprecated("Please use .merge(...) instead", "v2.0")
	def compareWith[R](f: A => A => R) = f(oldValue)(newValue)
	
	/**
	  * Merges the old and the new value together
	  * @param f A function that will produce the merge result
	  * @tparam B Type of the merge result
	  * @return The merge result
	  */
	def merge[B](f: (A, A) => B) = f(oldValue, newValue)
	/**
	  * Merges mapped values from the old and the new state together to form a third value
	  * @param map A mapping function applied to both the old and the new value
	  * @param merge A merge function that takes the mapped values and produces a result
	  * @tparam B Type of map result
	  * @tparam R Type of merge result
	  * @return Merge result
	  */
	def mergeBy[B, R](map: A => B)(merge: (B, B) => R) = merge(map(oldValue), map(newValue))
}
