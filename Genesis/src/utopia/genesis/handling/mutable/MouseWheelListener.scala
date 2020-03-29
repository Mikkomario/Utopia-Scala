package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.handling.MouseWheelHandlerType
import utopia.inception.handling.mutable.Handleable

/**
  * This is a mutable extension of the MouseWheelListener trait
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
trait MouseWheelListener extends handling.MouseWheelListener with Handleable
{
	/**
	  * @param newState Whether this instance is willing to receive more mouse wheel events
	  */
	def isReceivingMouseWheelEvents_=(newState: Boolean) = specifyHandlingState(MouseWheelHandlerType, newState)
}
