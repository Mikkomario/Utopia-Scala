package utopia.inception.handling.immutable

import utopia.inception.handling
import utopia.inception.handling.HandlerType

/**
  * This is an immutable extension of the Handleable trait
  * @author Mikko Hilpinen
  * @since 7.4.2019, v2+
  */
trait Handleable extends handling.Handleable
{
	/**
	  * @param handlerType The type of handler doing the handling
	  * @return Immutable handleable instances always allow handling
	  */
	override def allowsHandlingFrom(handlerType: HandlerType) = true
}
