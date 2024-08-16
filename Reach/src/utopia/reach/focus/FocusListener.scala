package utopia.reach.focus

import utopia.flow.view.mutable.Pointer
import utopia.reach.focus.FocusEvent.{FocusEntering, FocusGained, FocusLeaving, FocusLost}

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
	def apply[U](f: FocusEvent => U): FocusListener = new FunctionalFocusListener(f)
	
	/**
	  * @param focusPointer A focus pointer to manage / update
	  * @return A listener that updates the value of the specified focus pointer
	  */
	def managingFocusPointer(focusPointer: Pointer[Boolean]): FocusListener =
		new UpdatePointerFocusListener(focusPointer)
	
	/**
	  * @param f A function called when the listened item gains focus
	  * @tparam U Arbitrary function result type
	  * @return A new focus listener that only reacts to focus gained -events
	  */
	def onFocusGained[U](f: => U) = apply {
		case FocusGained => f
		case _ => ()
	}
	/**
	  * @param f A function called when the listened item loses focus
	  * @tparam U Arbitrary function result type
	  * @return A new focus listener that only reacts to focus lost -events
	  */
	def onFocusLost[U](f: => U) = apply {
		case FocusLost => f
		case _ => ()
	}
	/**
	  * @param f A function called when the listened item gains or loses focus.
	  *          Accepts a focus change event (i.e. FocusGained or FocusLost)
	  * @tparam U Arbitrary function result type
	  * @return A new focus listener that only reacts to focus change events
	  */
	def onFocusChanged[U](f: FocusChangeEvent => U) = apply {
		case FocusGained => f(FocusGained)
		case FocusLost => f(FocusLost)
		case _ => ()
	}
	/**
	  * @param f A function called when the listened item is about to gain or to lose focus.
	  *          Accepts true for FocusEntering events and false for FocusLeaving events
	  * @tparam U Arbitrary function result type
	  * @return A new focus listener
	  */
	def beforeFocusChange[U](f: Boolean => U) = apply {
		case FocusEntering => f(true)
		case FocusLeaving => f(false)
		case _ => ()
	}
	
	
	// NESTED	-----------------------------
	
	private class FunctionalFocusListener[U](f: FocusEvent => U) extends FocusListener
	{
		override def onFocusEvent(event: FocusEvent) = f(event)
	}
	
	private class UpdatePointerFocusListener(focusPointer: Pointer[Boolean]) extends FocusListener
	{
		override def onFocusEvent(event: FocusEvent): Unit = event match {
			case e: FocusChangeEvent => focusPointer.value = e.hasFocus
			case _ => ()
		}
	}
}

/**
  * A common trait for classes interested in all kinds of focus events
  * @author Mikko Hilpinen
  * @since 21.10.2020, v0.1
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
