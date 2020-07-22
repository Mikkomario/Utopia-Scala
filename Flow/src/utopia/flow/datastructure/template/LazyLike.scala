package utopia.flow.datastructure.template

/**
 * A common trait for lazily initialized containers
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1
 */
trait LazyLike[+A]
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
}
