package utopia.flow.view.template.eventful

import utopia.flow.util.logging.Logger

/**
  * Provides an immutable interface to a (mutable) changing item
  * @author Mikko Hilpinen
  * @since 31.8.2023, v2.2
  */
class ChangingView[+A](override protected val wrapped: Changing[A]) extends ChangingWrapper[A]
{
	// IMPLEMENTED  ----------------------
	
	override implicit def listenerLogger: Logger = wrapped.listenerLogger
	
	override def readOnly = this
}