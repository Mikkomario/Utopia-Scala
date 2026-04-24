package utopia.flow.util.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.view.immutable.View

object DelegatingLogger
{
	/**
	  * @param delegateView A view to the delegate that will perform the actual logging
	  * @return A new logger that delegates logging to whichever logger the specified view will point to
	  */
	def apply(delegateView: View[Logger]) = new DelegatingLogger(delegateView)
}

/**
  * A logger which delegates the logging to some other logger
  * @author Mikko Hilpinen
  * @since 13.01.2025, v2.5.1
  */
class DelegatingLogger(delegateView: View[Logger]) extends Logger
{
	override def apply(error: Option[Throwable], message: String, details: Model): Unit =
		delegateView.value(error, message, details)
}
