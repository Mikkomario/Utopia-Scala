package utopia.reflection.component.swing.template

import java.awt.Cursor
import java.awt.event.{FocusEvent, FocusListener, KeyEvent}

import utopia.reflection.util.AwtComponentExtensions._
import utopia.reflection.util.AwtEventThread

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
	  * @return Whether this component is in a visible component hierarchy
	  */
	def isInWindow = component.isInWindow
	
	/**
	  * @return The lowest window parent of this component. None if this component isn't hosted in any window.
	  */
	def parentWindow = component.parentWindow
	
	/**
	  * @return Whether this component is currently being displayed as a part of a visible component hierarchy
	  *         (visible hierarchy in visible window)
	  */
	def isInVisibleHierarchy = component.isInVisibleHierarchy
	
	
	// OTHER	--------------------
	
	/**
	  * Specifies that the mouse should have a hand cursor when hovering over this component
	  */
	def setHandCursor() = AwtEventThread.async { component.setCursor(
		Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)) }
	
	/**
	  * Specifies that the mouse should have the default cursor when hovering over this component
	  */
	def setArrowCursor() = AwtEventThread.async { component.setCursor(
		Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)) }
	
	/**
	  * Adds a new focus listener that is called when this component gains or loses focus
	  * @param onFocusGained Function called when this component gains focus
	  * @param onFocusLost Function called when this component loses focus
	  */
	def addFocusChangedListener(onFocusGained: => Unit)(onFocusLost: => Unit) = AwtEventThread.async {
		component.addFocusListener(new FunctionalFocusListener(Some(() => onFocusGained), Some(() => onFocusLost)))
	}
	
	/**
	  * Adds a new focus listener that is called when this component gains focus
	  * @param onFocusGained Function called when this component gains focus
	  */
	def addFocusGainedListener(onFocusGained: => Unit) = AwtEventThread.async {
		component.addFocusListener(new FunctionalFocusListener(Some(() => onFocusGained), None))
	}
	
	/**
	  * Adds a new focus listener that is called when this component loses focus
	  * @param onFocusLost Function called when this component loses focus
	  */
	def addFocusLostListener(onFocusLost: => Unit) = AwtEventThread.async {
		component.addFocusListener(new FunctionalFocusListener(None, Some(() => onFocusLost)))
	}
	
	/**
	  * Relays awt-originated key-events to another awt component
	  * @param anotherComponent Targeted awt component
	  */
	def relayAwtKeyEventsTo(anotherComponent: java.awt.Component) =
		AwtEventThread.async { component.addKeyListener(new KeyEventRelayer(anotherComponent)) }
	
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