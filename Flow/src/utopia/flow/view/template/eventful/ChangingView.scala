package utopia.flow.view.template.eventful

/**
  * Provides an immutable interface to a (mutable) changing item
  * @author Mikko Hilpinen
  * @since 31.8.2023, v2.2
  */
class ChangingView[+A](override protected val wrapped: Changing[A]) extends ChangingWrapper[A]