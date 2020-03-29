package utopia.flow.datastructure.mutable

/**
 * A common trait for lazily initialized mutable containers
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1+
 */
trait LazyLike[A]
{
	// ABSTRACT	-----------------------
	
	/**
	 * @return Current value of this lazily initialized container
	 */
	def current: Option[A]
	
	/**
	 * @return Value in this container (cached or generated)
	 */
	def get: A
	
	/**
	 * Overrides the current value of this lazy container
	 * @param newValue New value, which may be empty (uninitialized)
	 */
	protected def updateValue(newValue: Option[A]): Unit
	
	
	// OTHER	----------------------
	
	/**
	 * @param newValue New value for this lazy container
	 */
	def set(newValue: A) = updateValue(Some(newValue))
	
	/**
	 * Resets this lazy container so that a new value is generated the next time get is called
	 */
	def reset() = updateValue(None)
}
