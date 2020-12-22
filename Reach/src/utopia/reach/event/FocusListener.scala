package utopia.reach.event

import scala.language.implicitConversions

object FocusListener
{
	// IMPLICIT	-----------------------------
	
	// Automatically converts functions to focus listeners
	implicit def autoConvertFunction(f: FocusEvent => Unit): FocusListener = apply(f)
	
	
	// OTHER	-----------------------------
	
	/**
	  * Wraps a function into a focus listener
	  * @param f A function
	  * @return A focus listener
	  */
	def apply(f: FocusEvent => Unit): FocusListener = new FunctionalFocusListener(f)
	
	
	// NESTED	-----------------------------
	
	private class FunctionalFocusListener(f: FocusEvent => Unit) extends FocusListener
	{
		override def onFocusEvent(event: FocusEvent) = f(event)
	}
}

/**
  * A common trait for classes interested in all kinds of focus events
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  * @see FocusChangeListener
  */
trait FocusListener
{
	/**
	  * This method is called on received focus events
	  * @param event A focus event
	  */
	def onFocusEvent(event: FocusEvent): Unit
}
