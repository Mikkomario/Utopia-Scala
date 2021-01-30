package utopia.flow.event

/**
  * Change events are generated when a value changes
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1+
  * @tparam A the type of changed item
  * @param oldValue The previous value
  * @param newValue The new value
  */
case class ChangeEvent[+A](oldValue: A, newValue: A)
{
	// IMPLEMENTED	-----------------------------
	
	override def toString = s"Change from $oldValue to $newValue"
	
	
	// OTHER	---------------------------------
	
	/**
	  * Checks whether certain aspects of the old and new value are equal
	  * @param map A mapping function applied for both the old and the new value
	  * @tparam B Type of the mapped value
	  * @return True if the mapped values are equal, false otherwise
	  */
	def compareBy[B](map: A => B) = map(oldValue) == map(newValue)
	
	/**
	 * Checks whether certain aspects of the old and new value are different
	 * @param map A mapping function applied for both the old and the new value
	 * @tparam B Type of the mapped value
	 * @return True if the mapped values are different, false otherwise
	 */
	def differentBy[B](map: A => B) = !compareBy(map)
	
	/**
	  * Applies a function from the old value to the new value
	  * @param f A function for mapping into the applied function in the old value
	  * @tparam R Result type of the found function
	  * @return Result value of the found function when applied with the new value
	  */
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
