package utopia.flow.collection.immutable.caching.lazily

import utopia.flow.collection.mutable.caching.lazily.DeprecatingLazy
import utopia.flow.collection.template.caching.LazyLike

object Lazy
{
	/**
	  * @param make A function for filling this lazy container when a value is requested (only called up to once)
	  * @tparam A Type of wrapped value
	  * @return A new lazy container
	  */
	def apply[A](make: => A) = new Lazy[A](make)
	
	/**
	  * Creates a new lazy container that supports events
	  * @param make A function for generating the stored value when it is first requested
	  * @tparam A Type of wrapped value
	  * @return A new lazy container that supports events
	  */
	def listenable[A](make: => A) = ListenableLazy(make)
	/**
	  * @param make A function for creating an item when it is requested
	  * @tparam A Type of the item in this wrapper
	  * @return A lazily initialized wrapper that only holds a weak reference to the
	  *         generated item. A new item may be generated if the previous one is collected.
	  */
	def weak[A <: AnyRef](make: => A) = WeakLazy(make)
	/**
	 * Creates a new lazy container which may auto-reset itself under certain conditions
	 * @param make A function for generating a new value
	 * @param testValidity A function for testing whether a stored value is still valid (should be kept)
	 * @tparam A Type of stored item
	 * @return A new lazy container
	 */
	def deprecating[A](make: => A)(testValidity: A => Boolean) = DeprecatingLazy(make)(testValidity)
	/**
	  * @param make A function for creating an item when it is requested
	  * @param test A function that returns whether a generated value (accepted as a parameter) should be stored (true)
	  *             or whether a new value should be generated on the next call (false)
	  * @tparam A Type of stored / generated value
	  * @return A lazy container that only caches the value if it fulfills the specified test function
	  */
	def conditional[A](make: => A)(test: A => Boolean) = ConditionalLazy(make)(test)
	/**
	  * @param value A pre-calculated value
	  * @tparam A Type of that value
	  * @return That value wrapped as a lazy
	  */
	def wrap[A](value: A) = LazyWrapper(value)
}

/**
  * A view to a value that is lazily initialized
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
class Lazy[+A](generator: => A) extends LazyLike[A]
{
	// ATTRIBUTES	-------------------------
	
	private lazy val _value = generator
	private var initialized = false
	
	
	// IMPLEMENTED	-------------------------
	
	override def isInitialized = initialized
	
	override def nonInitialized = !initialized
	
	override def current = if (initialized) Some(_value) else None
	
	override def value =
	{
		if (!initialized)
			initialized = true
		_value
	}
}
