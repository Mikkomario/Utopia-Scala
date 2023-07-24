package utopia.flow.event.listener

import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}

import scala.language.implicitConversions

object ChangeListener
{
	/**
	  * Wraps a function into a change listener
	  * @param f A function that reacts to changes
	  *          and returns whether it should be informed of future change events as well
	  * @tparam A Type of changed item
	  * @return A new change listener based on the provided function
	  */
	def apply[A](f: ChangeEvent[A] => ChangeResponse): ChangeListener[A] = new FunctionalChangeListener[A](f)
	/**
	  * Wraps a function into a change listener
	  * @param f A function that reacts to change events
	  * @tparam A Type of changed item
	  * @tparam U Arbitrary function result type
	  * @return A new change listener based on the provided function.
	  *         This change listener continues to receive events until it is removed from the event source.
	  */
	def continuous[A, U](f: ChangeEvent[A] => U): ChangeListener[A] = apply { e =>
		f(e)
		Continue
	}
	/**
	  * Wraps a function into a one-time change listener.
	  * Once the listener has received a single change event, it is removed from the event source.
	  * @param f A function that reacts to change event(s)
	  * @tparam A Type of changed item
	  * @tparam U Arbitrary function result type
	  * @return A new change listener based on the specified function
	  */
	def once[A, U](f: ChangeEvent[A] => U) = apply[A] { e =>
		f(e)
		Detach
	}
	
	/**
	  * Wraps a function into a change listener
	  * @param f A function that doesn't use the change event but returns
	  *          whether it should be continued to be called in the future
	  * @return A new change listener that calls the specified function whenever a value changes
	  */
	def onAnyChange(f: => ChangeResponse): ChangeListener[Any] = new AnyChangeListener(f)
	/**
	  * Wraps a function into a change listener
	  * @param f A function that doesn't use the change event
	  * @return A new change listener
	  */
	def continuousOnAnyChange(f: => Unit): ChangeListener[Any] = onAnyChange {
		f
		Continue
	}
	
	
	// NESTED	-----------------------------------
	
	private class FunctionalChangeListener[-A](f: ChangeEvent[A] => ChangeResponse) extends ChangeListener[A]
	{
		override def onChangeEvent(event: ChangeEvent[A]) = f(event)
	}
	private class AnyChangeListener(f: => ChangeResponse) extends ChangeListener[Any]
	{
		override def onChangeEvent(event: ChangeEvent[Any]) = f
	}
}

/**
  * This listeners are interested in change events
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1+
  * @tparam A The type of changed item
  */
trait ChangeListener[-A]
{
	/**
	  * This method is called when a value changes
	  * @param event The change event
	  * @return A response that dictates whether this listener should continue to be informed of change events
	  *         in the future, and whether any additional actions should be performed afterwards.
	  */
	def onChangeEvent(event: ChangeEvent[A]): ChangeResponse
}
