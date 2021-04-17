package utopia.flow.datastructure.mutable

object ResettableLazy
{
	/**
	  * Creates a new lazily initialized wrapper
	  * @param make A function for generating the wrapped item when it is requested (may be called multiple times)
	  * @tparam A Type of wrapped item
	  * @return A new lazy wrapper
	  */
	def apply[A](make: => A) = new ResettableLazy[A](make)
}

/**
  * This lazily initialized container allows one to request a new value initialization
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
class ResettableLazy[A](generator: => A) extends ResettableLazyLike[A]
{
	// ATTRIBUTES	---------------------------
	
	private var _value: Option[A] = None
	
	
	// IMPLEMENTED	---------------------------
	
	override def reset() = _value = None
	
	override def current = _value
	
	override def value = _value match
	{
		case Some(value) => value
		case None =>
			val newValue = generator
			_value = Some(newValue)
			newValue
	}
}
