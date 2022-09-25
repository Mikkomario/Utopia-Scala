package utopia.flow.view.mutable.caching

import utopia.flow.view.mutable.Pointer

/**
  * A common trait for lazy container implementations which allow outside manipulation
  * @author Mikko Hilpinen
  * @since 22.7.2020, v1.8
  */
trait MutableLazyLike[A] extends ResettableLazyLike[A] with Pointer[A]
{
	// OTHER	----------------------
	
	/**
	  * @param newValue New value for this lazy container
	  */
	@deprecated("Please assign directly to .value instead", "v1.9")
	def set(newValue: A) = value = newValue
}
