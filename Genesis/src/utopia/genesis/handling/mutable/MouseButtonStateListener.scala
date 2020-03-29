package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.handling.MouseButtonStateHandlerType
import utopia.inception.handling.mutable.Handleable

/**
  * This is a mutable extension of the MouseButtonStateListener trait
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
trait MouseButtonStateListener extends handling.MouseButtonStateListener with Handleable
{
	/**
	  * @param newState New mouse button state handling state
	  */
	def isReceivingMouseButtonStateEvents_=(newState: Boolean) = specifyHandlingState(MouseButtonStateHandlerType, newState)
}
