package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.handling.ActorHandlerType
import utopia.inception.handling.mutable.Handleable

/**
  * This is a mutable extension of the Actor trait
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait Actor extends handling.Actor with Handleable
{
	/**
	  * @param newState New state for actor handler type
	  */
	def isActive_=(newState: Boolean) = specifyHandlingState(ActorHandlerType, newState)
}
