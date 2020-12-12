package utopia.reflection.event

import utopia.reflection.event.FocusEvent.{FocusGained, FocusLost}

import scala.language.implicitConversions

object FocusChangeListener
{
	// IMPLICIT	------------------------------------------
	
	implicit def autoConvertFunction(f: FocusChangeEvent => Unit): FocusChangeListener = apply(f)
	
	
	// OTHER	------------------------------------------
	
	/**
	  * @param f A function that accepts focus change events
	  * @return A focus change listener based on the function
	  */
	def apply(f: FocusChangeEvent => Unit): FocusChangeListener = new FunctionalFocusChangeListener(f)
	
	/**
	  * @param f A function to call on focus gain events
	  * @return A focus listener based on the function
	  */
	def onFocusGain(f: => Unit) = FocusListener {
		case FocusGained => f
		case _ => ()
	}
	
	/**
	  * @param f A function to call on focus lost events
	  * @return A focus listener based on the function
	  */
	def onFocusLost(f: => Unit) = FocusListener
	{
		case FocusLost => f
		case _ => ()
	}
	
	
	// NESTED	------------------------------------------
	
	private class FunctionalFocusChangeListener(f: FocusChangeEvent => Unit) extends FocusChangeListener
	{
		override def onFocusChangeEvent(event: FocusChangeEvent) = f(event)
	}
}

/**
  * Common trait for classes interested in focus change events
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
trait FocusChangeListener extends FocusListener
{
	// ABSTRACT	-----------------------------------------
	
	/**
	  * This method is called when a focus change event is recognized
	  * @param event A focus change event
	  */
	def onFocusChangeEvent(event: FocusChangeEvent): Unit
	
	
	// IMPLEMENTED	-------------------------------------
	
	// Ignores non-change events
	override def onFocusEvent(event: FocusEvent) = event match
	{
		case e: FocusChangeEvent => onFocusChangeEvent(e)
		case _ => ()
	}
}
