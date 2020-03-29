package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.handling.KeyTypedHandlerType
import utopia.inception.handling.mutable.Handleable

/**
  * This is a mutable extension of the KeyTypedListener trait
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
trait KeyTypedListener extends handling.KeyTypedListener with Handleable
{
	/**
	  * @param newState Whether this instance is willing to receive more key typed events
	  */
	def isReceivingKeyTypedEvents_=(newState: Boolean) = specifyHandlingState(KeyTypedHandlerType, newState)
}
