package utopia.genesis.handling.template

import utopia.flow.view.template.eventful.FlagLike

/**
  * Common trait for items which may be placed within handlers
  * @author Mikko Hilpinen
  * @since 30/01/2024, v4.0
  */
trait Handleable2
{
	// ABSTRACT ---------------------
	
	/**
	  * @return A condition that must be met in order for this item to be handled.
	  *         The [[Handler2]] implementations *should* respect this condition.
	  */
	def handleCondition: FlagLike
}
