package utopia.flow.collection.immutable.caching.lazily

import utopia.flow.collection.template.caching.LazyLike

object ConditionalLazy
{
	/**
	  * @param generate A function for generating a new value when this lazy is called. Also returns whether the
	  *                 value should be stored in this container (true) or whether a new value should be generated
	  *                 on next call (false).
	  * @tparam A Type of value stored / produced by this lazy container
	  * @return A lazy container that caches the first value that is accompanied with 'true' as the second value.
	  */
	def apply[A](generate: => (A, Boolean)) = new ConditionalLazy[A](generate)
	/**
	  * @param generate A function for generating new value(s)
	  * @param test A function that accepts a generated value and returns whether it should be stored (true) or
	  *             whether a new value should be generated on the next call (false)
	  * @tparam A Type of stored value
	  * @return A lazy container that caches the first generated value that is accepted by the specified test function
	  */
	def apply[A](generate: => A)(test: A => Boolean): ConditionalLazy[A] = apply {
		val value = generate
		value -> test(value)
	}
}

/**
  * This lazy container only stores the value when a certain condition is met
  * @author Mikko Hilpinen
  * @since 7.11.2021, v1.14.1
  */
class ConditionalLazy[A](generate: => (A, Boolean)) extends LazyLike[A]
{
	// ATTRIBUTES   ----------------------------
	
	private var stored: Option[A] = None
	
	
	// IMPLEMENTED  ----------------------------
	
	override def current = stored
	
	override def value = stored.getOrElse {
		// Stores the generated value based on the additional flag
		val (newValue, store) = generate
		if (store)
			stored = Some(newValue)
		newValue
	}
}
