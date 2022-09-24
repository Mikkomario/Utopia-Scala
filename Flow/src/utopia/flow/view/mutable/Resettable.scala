package utopia.flow.view.mutable

/**
  * A common trait for containers / items which have a mutable state that may be reset
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait Resettable
{
	/**
	  * Resets this item to it's original state
	  * @return Whether this item's state changed
	  */
	def reset(): Boolean
}
