package utopia.reach.component.factory

/**
  * Common trait for component factories (and other classes) that wrap a component creation context
  * @author Mikko Hilpinen
  * @since 13.5.2023, v1.1
  */
trait HasContext[+N] extends Any
{
	/**
	  * @return The component creation context wrapped by this instance
	  */
	def context: N
}
