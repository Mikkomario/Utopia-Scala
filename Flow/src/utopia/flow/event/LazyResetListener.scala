package utopia.flow.event

import scala.language.implicitConversions

object LazyResetListener
{
	// IMPLICIT ------------------------------
	
	implicit def functionToListener[A](f: A => Unit): LazyResetListener[A] = apply(f)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param f A function to all on each lazy reset
	  * @tparam A Type of the value accepted by the function
	  * @return A new lazy reset listener
	  */
	def apply[A](f: A => Unit): LazyResetListener[A] = new FunctionalResetListener[A](f)
	
	/**
	  * @param f A function to call on lazy reset
	  * @return A new lazy reset listener
	  */
	def onAnyReset(f: => Unit) = apply[Any] { _  => f }
	
	
	// NESTED   ------------------------------
	
	private class FunctionalResetListener[-A](f: A => Unit) extends LazyResetListener[A]
	{
		override def onReset(oldValue: A) = f(oldValue)
	}
}

/**
  * A listener that is interested in reset events in ListenableResettableLazy instances
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.9.2
  */
trait LazyResetListener[-A]
{
	/**
	  * This method is called when a value in a resettable lazy instance is being reset
	  * @param oldValue Value that was held within the lazy before the reset
	  */
	def onReset(oldValue: A): Unit
}
