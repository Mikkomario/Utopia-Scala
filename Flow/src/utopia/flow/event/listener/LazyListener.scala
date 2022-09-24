package utopia.flow.event.listener

import scala.language.implicitConversions

object LazyListener
{
	// IMPLICIT --------------------------------
	
	implicit def functionToListener[A](f: A => Unit): LazyListener[A] = apply[A](f)
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param f A function to call when a new value is generated in a lazy container
	  * @tparam A Type of values being listened
	  * @return A new listener instance
	  */
	def apply[A](f: A => Unit): LazyListener[A] = new FunctionalLazyListener[A](f)
	
	/**
	  * @param f A function to call when a new value is generated in a lazy container
	  * @return A new listener instance
	  */
	def onAnyValueGeneration(f: => Unit) = apply[Any] { _ => f }
	
	
	// NESTED   --------------------------------
	
	private class FunctionalLazyListener[-A](f: A => Unit) extends LazyListener[A]
	{
		override def onValueGenerated(newValue: A) = f(newValue)
	}
}

/**
  * Common trait for listeners that are interested in events concerning lazy instances
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait LazyListener[-A]
{
	/**
	  * This method is called when a new value is generated on a lazy instance this listener is
	  * listening
	  * @param newValue The new value that was generated and stored in the lazy instance
	  */
	def onValueGenerated(newValue: A): Unit
}
