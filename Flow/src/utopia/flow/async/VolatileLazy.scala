package utopia.flow.async

import utopia.flow.datastructure.template.LazyLike

object VolatileLazy
{
	/**
	  * @param make A function for creating the value when it is first requested (only called once)
	  * @tparam A Type of wrapped value
	  * @return A new lazily initialized, thread-safe value wrapper
	  */
	def apply[A](make: => A) = new VolatileLazy[A](make)
}

/**
  * Used when you need a thread-safe lazily initialized value
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
class VolatileLazy[A](generator: => A) extends LazyLike[A]
{
	// ATTRIBUTES	--------------------------------
	
	@volatile private var _value: Option[A] = None
	
	
	// IMPLEMENTED	--------------------------------
	
	override def current = this.synchronized { _value }
	
	override def value = this.synchronized {
		_value match
		{
			case Some(value) => value
			case None =>
				val newValue = generator
				_value = Some(newValue)
				newValue
		}
	}
}
