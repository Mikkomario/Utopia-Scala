package utopia.flow.view.mutable.caching

object ResettableVolatileLazy
{
	/**
	  * @param make A function for generating a new value when it is requested (may be called multiple times)
	  * @tparam A Type of generated value
	  * @return A lazily initialized thread-safe wrapper
	  */
	def apply[A](make: => A) = new ResettableVolatileLazy[A](make)
}

/**
  * Used when you need a resettable thread safe container
  * @author Mikko Hilpinen
  * @since 4.11.2020, v1.9
  */
class ResettableVolatileLazy[A](generator: => A) extends ResettableLazy[A]
{
	// ATTRIBUTES	-------------------------
	
	private val wrapped = MutableVolatileLazy(generator)
	
	
	// IMPLEMENTED	-------------------------
	
	override def reset() = wrapped.reset()
	
	override def current = wrapped.current
	
	override def value = wrapped.value
}
