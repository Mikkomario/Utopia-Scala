package utopia.reflection.component.swing

import java.awt.Cursor
import java.awt.event.{FocusEvent, FocusListener, KeyEvent}

/**
  * This trait is extended by classes that have a related awt component
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait AwtComponentRelated
{
	// ABSTRACT	--------------------
	
	/**
	  * @return The awt component associated with this instance
	  */
	def component: java.awt.Component
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return The lowest window parent of this component. None if this component isn't hosted in any window.
	  */
	def parentWindow =
	{
		var nextParent = component.getParent
		var window: Option[java.awt.Window] = None
		
		// Checks parents until a window is found
		while (window.isEmpty && nextParent != null)
		{
			nextParent match
			{
				case w: java.awt.Window => window = Some(w)
				case _ => nextParent = nextParent.getParent
			}
		}
		
		window
	}
	
	
	// OTHER	--------------------
	
	/**
	  * Specifies that the mouse should have a hand cursor when hovering over this component
	  */
	def setHandCursor() = component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
	
	/**
	  * Specifies that the mouse should have the default cursor when hovering over this component
	  */
	def setArrowCursor() = component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
	
	/**
	  * Adds a new focus listener that is called when this component gains or loses focus
	  * @param onFocusGained Function called when this component gains focus
	  * @param onFocusLost Function called when this component loses focus
	  */
	def addFocusChangedListener(onFocusGained: => Unit)(onFocusLost: => Unit) = component.addFocusListener(
		new FunctionalFocusListener(Some(() => onFocusGained), Some(() => onFocusLost)))
	
	/**
	  * Adds a new focus listener that is called when this component gains focus
	  * @param onFocusGained Function called when this component gains focus
	  */
	def addFocusGainedListener(onFocusGained: => Unit) = component.addFocusListener(
		new FunctionalFocusListener(Some(() => onFocusGained), None))
	
	/**
	  * Adds a new focus listener that is called when this component loses focus
	  * @param onFocusLost Function called when this component loses focus
	  */
	def addFocusLostListener(onFocusLost: => Unit) = component.addFocusListener(
		new FunctionalFocusListener(None, Some(() => onFocusLost)))
	
	/**
	  * Relays awt-originated key-events to another awt component
	  * @param anotherComponent Targeted awt component
	  */
	def relayAwtKeyEventsTo(anotherComponent: java.awt.Component) =
		component.addKeyListener(new KeyEventRelayer(anotherComponent))
	
	/**
	  * Relays awt-originated key-events to another awt component
	  * @param anotherComponent Targeted awt component wrapper
	  */
	def relayAwtKeyEventsTo(anotherComponent: AwtComponentRelated): Unit = relayAwtKeyEventsTo(anotherComponent.component)
	
	/**
	  * @return Whether this component is currently in focus
	  */
	def isInFocus = component.isFocusOwner
}

private class FunctionalFocusListener(onFocusGained: Option[() => Unit], onFocusLost: Option[() => Unit]) extends FocusListener
{
	override def focusGained(e: FocusEvent) = onFocusGained.foreach { _() }
	
	override def focusLost(e: FocusEvent) = onFocusLost.foreach { _() }
}

private class KeyEventRelayer(targetComponent: java.awt.Component) extends java.awt.event.KeyListener
{
	override def keyTyped(e: KeyEvent) = relayEvent(e)
	
	override def keyPressed(e: KeyEvent) = relayEvent(e)
	
	override def keyReleased(e: KeyEvent) = relayEvent(e)
	
	private def relayEvent(e: KeyEvent) = targetComponent.dispatchEvent(e)
}