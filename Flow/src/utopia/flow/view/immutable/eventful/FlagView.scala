package utopia.flow.view.immutable.eventful

import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.SettableFlag
import utopia.flow.view.template.eventful.{ChangingWrapper, Flag}

/**
  * An immutable view to a changing (mutable) flag
  * @author Mikko Hilpinen
  * @since 18.9.2022, v1.17
  */
class FlagView(flag: SettableFlag) extends Flag with ChangingWrapper[Boolean]
{
	// IMPLEMENTED  -------------------
	
	override implicit def listenerLogger: Logger = flag.listenerLogger
	override protected def wrapped = flag
	
	override def readOnly = this
}
