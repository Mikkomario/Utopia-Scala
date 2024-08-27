package utopia.flow.view.mutable

import utopia.flow.view.template.MaybeSet

/**
  * A common trait for containers / items which have a mutable state that may be reset
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
trait Resettable extends MaybeSet
{
	/**
	  * Resets this item to its original state
	  * @return Whether this item's state changed
	  */
	def reset(): Boolean
}
