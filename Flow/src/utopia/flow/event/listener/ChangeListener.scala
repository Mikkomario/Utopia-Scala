package utopia.flow.event.listener

import utopia.flow.event.DetachmentChoice
import utopia.flow.event.model.{ChangeEvent, DetachmentChoice}

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
	def apply[A](f: ChangeEvent[A] => DetachmentChoice): ChangeListener[A] = new FunctionalChangeListener[A](f)
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
		DetachmentChoice.continue
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
		DetachmentChoice.detach
	}
	
	/**
	  * Wraps a function into a change listener
	  * @param f A function that doesn't use the change event but returns
	  *          whether it should be continued to be called in the future
	  * @return A new change listener that calls the specified function whenever a value changes
	  */
	def onAnyChange(f: => DetachmentChoice): ChangeListener[Any] = new AnyChangeListener(f)
	/**
	  * Wraps a function into a change listener
	  * @param f A function that doesn't use the change event
	  * @return A new change listener
	  */
	def continuousOnAnyChange(f: => Unit): ChangeListener[Any] = onAnyChange {
		f
		DetachmentChoice.continue
	}
	
	
	// NESTED	-----------------------------------
	
	private class FunctionalChangeListener[-A](f: ChangeEvent[A] => DetachmentChoice) extends ChangeListener[A]
	{
		override def onChangeEvent(event: ChangeEvent[A]) = f(event)
	}
	private class AnyChangeListener(f: => DetachmentChoice) extends ChangeListener[Any]
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
	  * @return Whether this listener should be informed of future changes, also
	  */
	def onChangeEvent(event: ChangeEvent[A]): DetachmentChoice
}
