package utopia.flow.datastructure.mutable

object DeprecatingLazy
{
	/**
	 * Creates a new lazy container which may auto-reset itself under certain conditions
	 * @param make A function for generating a new value
	 * @param testValidity A function for testing whether a stored value is still valid (should be kept)
	 * @tparam A Type of stored item
	 * @return A new lazy container
	 */
	def apply[A](make: => A)(testValidity: A => Boolean) = new DeprecatingLazy[A](make, testValidity)
}

/**
 * A lazy container which may consider its contents deprecated after a while (at which point they are auto-reset)
 * @author Mikko Hilpinen
 * @since 27.2.2022, v1.15
 */
class DeprecatingLazy[A](generator: => A, tester: A => Boolean) extends ResettableLazyLike[A]
{
	// ATTRIBUTES   --------------------------
	
	private val cache = ResettableLazy(generator)
	
	
	// IMPLEMENTED  --------------------------
	
	override def current = cache.current.flatMap { current =>
		// Case: Current item is still valid
		if (tester(current))
			Some(current)
		// Case: Current item has deprecated => resets the underlying cache
		else {
			reset()
			None
		}
	}
	
	// Tests for deprecation at every call
	override def value = current.getOrElse { cache.value }
	
	override def reset() = cache.reset()
}
