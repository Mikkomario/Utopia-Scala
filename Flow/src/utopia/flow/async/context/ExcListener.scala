package utopia.flow.async.context

import scala.language.implicitConversions

object ExcListener
{
	// OTHER    ------------------------
	
	/**
	  * Implicitly converts a function into a listener
	  * @param f A function called on [[ExcEvent]]s.
	  * @tparam U Arbitrary function return type
	  * @return A new listener which calls the specified function
	  */
	implicit def apply[U](f: ExcEvent => U): ExcListener = new _ExcListener[U](f)
	
	
	// NESTED   ------------------------
	
	private class _ExcListener[U](f: ExcEvent => U) extends ExcListener
	{
		override def onExcEvent(event: ExcEvent): Unit = f(event)
	}
}

/**
  * Common trait for listeners that want to receive events about execution contexts (in Flow, that is [[ThreadPool]]).
  * @author Mikko Hilpinen
  * @since 23.09.2024, v2.5
  */
trait ExcListener
{
	/**
	  * Accepts a recent event from an execution context.
	  * Note: Be very careful not to, in this method, utilize the execution context that created this event.
	  *       Otherwise, an infinite look will follow.
	  * @param event The event that occurred
	  */
	def onExcEvent(event: ExcEvent): Unit
}
