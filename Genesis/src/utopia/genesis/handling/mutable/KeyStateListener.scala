package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.handling.KeyStateHandlerType
import utopia.inception.handling.mutable.Handleable

/**
  * This is a mutable extension of the KeyStateListener trait
  * @author Mikko Hilpinen
  * @since 20.4.2019, v2+
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
trait KeyStateListener extends handling.KeyStateListener with Handleable
{
	/**
	  * @param newState Whether this instance should be informed of new key state events
	  */
	def isReceivingKeyStateEvents_=(newState: Boolean) = specifyHandlingState(KeyStateHandlerType, newState)
}
