package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.handling.MouseMoveHandlerType
import utopia.inception.handling.mutable.Handleable

/**
  * This is a mutable extension of the MouseMoveListener trait
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
trait MouseMoveListener extends handling.MouseMoveListener with Handleable
{
	/**
	  * @param newState Whether this instance is willing to receive more mouse move events
	  */
	def isReceivingMouseMoveEvents_=(newState: Boolean) = specifyHandlingState(MouseMoveHandlerType, newState)
}
