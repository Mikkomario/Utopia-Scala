package utopia.flow.event

import scala.language.implicitConversions

object ChangeListener
{
	implicit def functionToListener[A](f: ChangeEvent[A] => Unit): ChangeListener[A] = apply(f)
	
	/**
	  * Wraps a function into a change listener
	  * @param f A function
	  * @tparam A Type of changed item
	  * @return A new change listener based on the provided function
	  */
	def apply[A](f: ChangeEvent[A] => Unit): ChangeListener[A] = new FunctionalChangeListener[A](f)
	
	/**
	  * Wraps a function into a change listener
	  * @param f A function that doesn't use the change event
	  * @return A new change listener that calls the specified function whenever a value changes
	  */
	def onAnyChange(f: => Unit): ChangeListener[Any] = new AnyChangeListener(f)
	
	
	// NESTED	-----------------------------------
	
	private class FunctionalChangeListener[-A](f: ChangeEvent[A] => Unit) extends ChangeListener[A]
	{
		override def onChangeEvent(event: ChangeEvent[A]) = f(event)
	}
	
	private class AnyChangeListener(f: => Unit) extends ChangeListener[Any]
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
	  */
	def onChangeEvent(event: ChangeEvent[A]): Unit
}
