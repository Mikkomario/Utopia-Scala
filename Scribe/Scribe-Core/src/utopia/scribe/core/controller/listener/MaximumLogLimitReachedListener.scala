package utopia.scribe.core.controller.listener

import utopia.scribe.core.model.cached.event.MaximumLogLimitReachedEvent

import scala.language.implicitConversions

object MaximumLogLimitReachedListener
{
	// IMPLICIT -------------------------
	
	implicit def functionToListener(f: MaximumLogLimitReachedEvent => Unit): MaximumLogLimitReachedListener = apply(f)
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a new functional listener
	  * @param f A function that should be called when/if the logging limit is reached
	  * @tparam U Arbitrary function result type
	  * @return A new listener instance
	  */
	def apply[U](f: MaximumLogLimitReachedEvent => U): MaximumLogLimitReachedListener =
		new _MaximumLogLimitReachedListener(f)
	
	
	// NESTED   -------------------------
	
	private class _MaximumLogLimitReachedListener[U](f: MaximumLogLimitReachedEvent => U)
		extends MaximumLogLimitReachedListener
	{
		override def onLogLimitReached(event: MaximumLogLimitReachedEvent): Unit = f(event)
	}
}

/**
  * Common trait for those who are interested in receiving events when the maximum logging limit is reached.
  * @author Mikko Hilpinen
  * @since 3.8.2023, v1.0
  */
trait MaximumLogLimitReachedListener
{
	/**
	  * Called when the maximum logging limit is reached
	  * @param event An event containing some information about this situation
	  */
	def onLogLimitReached(event: MaximumLogLimitReachedEvent): Unit
}
