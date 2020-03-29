package utopia.flow.async

import utopia.flow.datastructure.mutable.LazyLike

object VolatileLazy
{
	/**
	 * @param makeNew A function for producing a new value for this lazy (call by name)
	 * @tparam A Type of contained value
	 * @return A new lazily initialized container
	 */
	def apply[A](makeNew: => A) = new VolatileLazy[A](() => makeNew)
}

/**
 * Used when you need a thread-safe lazily initialized value
 * @author Mikko Hilpinen
 * @since 17.12.2019, v1.6.1+
 */
class VolatileLazy[A](private val generator: () => A) extends LazyLike[A]
{
	// ATTRIBUTES	---------------------
	
	private val value: VolatileOption[A] = VolatileOption()
	
	
	// IMPLEMENTED	--------------------
	
	override def current = value.get
	
	override protected def updateValue(newValue: Option[A]) = value.set(newValue)
	
	def get = value.setOneIfEmptyAndGet(generator)
}
