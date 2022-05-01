package utopia.flow.datastructure.template

import utopia.flow.datastructure.mutable.PollableOnce

/**
 * A common trait for lazily initialized value wrappers
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1
  * @tparam A Type of wrapped value
 */
trait LazyLike[+A] extends Viewable[A]
{
	// ABSTRACT	-----------------------
	
	/**
	 * @return Current value of this lazily initialized container
	 */
	def current: Option[A]
	
	
	// COMPUTED	------------------------
	
	/**
	 * @return Value in this container (cached or generated)
	 */
	@deprecated("Please use .value instead", "v1.9")
	def get = value
	
	/**
	  * @return Whether this lazily initialized wrapper has already been initialized
	  */
	def isInitialized = current.nonEmpty
	/**
	  * @return Whether this lazily initialized wrapper hasn't been initialized yet
	  */
	def nonInitialized = current.isEmpty
	
	/**
	  * @return A lazily initialized iterator based on the contents of this lazy container
	  */
	def iterator = PollableOnce { value }
	
	
	// IMPLEMENTED	---------------------
	
	override def toString = current.map(c => s"Lazy($c)") getOrElse "Lazy"
}
