package utopia.genesis.handling.template

import utopia.flow.view.template.eventful.FlagLike

/**
  * Common trait for items which may be placed within handlers
  * @author Mikko Hilpinen
  * @since 30/01/2024, v4.0
  */
trait Handleable
{
	// ABSTRACT ---------------------
	
	/**
	  * @return A condition that must be met in order for this item to be handled.
	  *         The [[Handler]] implementations *should* respect this condition.
	  */
	def handleCondition: FlagLike
	
	
	// COMPUTED --------------------
	
	/**
	  * @return Whether it is okay to handle / interact with this item at this time
	  */
	def mayBeHandled = handleCondition.value
	/**
	  * @return Whether it is not okay to handle / interact with this item at this time
	  */
	def mayNotBeHandled = !mayBeHandled
}
