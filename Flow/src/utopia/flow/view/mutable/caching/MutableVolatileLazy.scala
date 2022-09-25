package utopia.flow.view.mutable.caching

import utopia.flow.view.mutable.async.VolatileOption

object MutableVolatileLazy
{
	/**
	 * @param makeNew A function for producing a new value for this lazy (call by name)
	 * @tparam A Type of contained value
	 * @return A new lazily initialized container
	 */
	def apply[A](makeNew: => A) = new MutableVolatileLazy[A](makeNew)
}

/**
 * Used when you need a mutable thread-safe lazily initialized container
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1+
 */
class MutableVolatileLazy[A](generator: => A) extends MutableLazy[A]
{
	// ATTRIBUTES	---------------------
	
	private val wrapped: VolatileOption[A] = VolatileOption()
	
	
	// IMPLEMENTED	--------------------
	
	override def value_=(newValue: A) = wrapped.setOne(newValue)
	
	override def reset() = wrapped.clear()
	
	override def value = wrapped.setOneIfEmpty { generator }
	
	override def current = wrapped.value
}
