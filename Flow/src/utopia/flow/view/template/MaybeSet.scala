package utopia.flow.view.template

/**
  * Common trait for items which may be in a binary "set" / on state
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
trait MaybeSet extends Any
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Whether this item is currently "set" / on
	  */
	def isSet: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Whether this item is not currently "set" / on
	  */
	def isNotSet = !isSet
	
	
	// OTHER    ------------------------
	
	/**
	 * If this has been set, fails, otherwise calls the specified function
	 * @param f A function to call if this item has not been set
	 * @tparam A Type of 'f' results
	 * @throws IllegalStateException If this item has already been set
	 * @return Result of 'f'
	 */
	@throws[IllegalStateException]("If this item has already been set")
	def failIfSet[A](f: => A) = {
		if (isSet)
			throw new IllegalStateException("This item has already been set")
		else
			f
	}
	/**
	 * Calls the specified function, but only if this item has not been set
	 * @param f Function to call if this has not been set
	 * @tparam U Arbitrary result type of 'f'
	 * @return Whether 'f' was called
	 */
	def ifNotSet[U](f: => U) = {
		if (isSet)
			false
		else {
			f
			true
		}
	}
}
