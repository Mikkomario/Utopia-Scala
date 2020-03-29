package utopia.genesis.handling.mutable

import utopia.genesis.handling
import utopia.genesis.handling.DrawableHandlerType
import utopia.inception.handling.mutable.Handleable

/**
  * This is a mutable extension of the Drawable trait
  * @author Mikko
  * @since 20.4.2019, v2+
  */
trait Drawable extends handling.Drawable with Handleable
{
	/**
	  * @param newState Whether this instance should be drawn
	  */
	def isVisible_=(newState: Boolean) = specifyHandlingState(DrawableHandlerType, newState)
}
