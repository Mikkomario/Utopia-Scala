package utopia.flow.event

/**
  * A common trait for listeners that are interested in value generation and reset events
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait ResettableLazyListener[-A] extends LazyListener[A]
{
	/**
	  * This method is called when a value in a resettable lazy instance is being reset
	  * @param oldValue Value that was held within the lazy before the reset
	  */
	def onReset(oldValue: A): Unit
}
