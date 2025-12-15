package utopia.flow.view.immutable.eventful

import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.{ChangingWrapper, Flag}

/**
  * An immutable view to a changing (mutable) flag
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
class FlagView(protected val wrapped: Flag) extends Flag with ChangingWrapper[Boolean]
{
	// IMPLEMENTED  -------------------
	
	override implicit def listenerLogger: Logger = wrapped.listenerLogger
	
	override def readOnly = this
	override def toString = wrapped.toString
}
