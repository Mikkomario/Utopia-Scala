package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template.LazyLike

object Lazy
{
	/**
	  * @param make A function for filling this lazy container when a value is requested (only called up to once)
	  * @tparam A Type of wrapped value
	  * @return A new lazy container
	  */
	def apply[A](make: => A) = new Lazy[A](make)
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
