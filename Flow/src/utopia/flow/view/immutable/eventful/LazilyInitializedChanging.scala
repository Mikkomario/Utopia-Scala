package utopia.flow.view.immutable.eventful

import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

object LazilyInitializedChanging
{
	/**
	  * @param initialize A lazily called function that initializes the wrapped pointer
	  * @tparam A Type of changing values
	  * @return A new lazily initialized changing wrapper
	  */
	def apply[A](initialize: => Changing[A]) = new LazilyInitializedChanging[A](initialize)
}

/**
  * A changing item that's lazily initialized.
  *
  * Useful in situations where the pointer provider doesn't know whether the pointer will be actually utilized
  * (as initializing a pointer may cost resources and create additional dependencies to other pointers).
  *
  * @author Mikko Hilpinen
  * @since 19.12.2024, v2.5.1
  */
class LazilyInitializedChanging[+A](initialize: => Changing[A]) extends ChangingWrapper[A]
{
	override protected lazy val wrapped: Changing[A] = initialize
	override implicit def listenerLogger: Logger = wrapped.listenerLogger
}
