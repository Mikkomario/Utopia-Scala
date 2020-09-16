package utopia.reflection.component.swing.button

import java.awt.event.{FocusEvent, FocusListener, KeyEvent}

import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButton, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.inception.handling.HandlerType
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.{ComponentLike, Focusable}

object ButtonLike
{
	private val defaultTriggerKeys = Set(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE)
}

/**
  * Used as a common trait for all different button implementations
  * @author Mikko Hilpinen
  * @since 25.4.2019, v1+
  */
// TODO: Add support for hotkeys
trait ButtonLike extends ComponentLike with AwtComponentRelated with Focusable
{
	import ButtonLike._
	
	// ABSTRACT	----------------------
	
	/**
	  * Updates this button's style to match the new state
	  * @param newState New button state
	  */
	protected def updateStyleForState(newState: ButtonState): Unit
	
	
	// ATTRIBUTES	------------------
	
	private var actions = Vector[() => Unit]()
	private var _state = ButtonState.default
	
	
	// COMPUTED	----------------------
	
	/**
	  * @return Whether this button is currently enabled
	  */
	def enabled = component.isEnabled
	def enabled_=(newEnabled: Boolean) =
	{
		state = _state.copy(isEnabled = newEnabled)
		component.setEnabled(newEnabled)
	}
	
	/**
	  * @return Whether this button is currently enabled
	  */
	def isEnabled = enabled
	/**
	  * @param newEnabled Whether this button should be enabled
	  */
	@deprecated("Replaced with enabled = ...", "v1.2")
	def isEnabled_=(newEnabled: Boolean) = enabled = newEnabled
	
	/**
	  * @return This button's current state
	  */
	def state = _state
	private def state_=(newState: ButtonState) =
	{
		_state = newState
		updateStyleForState(newState)
	}
	
	private def down = state.isPressed
	private def down_=(newState: Boolean) = if (down != newState) state = state.copy(isPressed = newState)
	
	
	// IMPLEMENTED	------------------
	
	override def requestFocusInWindow() = component.requestFocusInWindow()
	
	override def isInFocus = state.isInFocus
	private def updateFocus(newFocusState: Boolean) = if (isInFocus != newFocusState) state = state.copy(isInFocus = newFocusState)
	
	
	// OTHER	----------------------
	
	/**
	  * Adds a new action to this button. The action will be performed whenever this button is pressed
	  * @param action An action that should be performed when this button is pressed
	  */
	def registerAction(action: () => Unit) = actions :+= action
	
	/**
	  * Triggers this button's action. Same as if the user clicked this button (only works for enabled buttons)
	  */
	def trigger() = if (isEnabled) actions.foreach { _() }
	
	/**
	  * Initializes this button's listeners. Should be called when constructing this button.
	  */
	protected def initializeListeners(hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set()) =
	{
		// Adds mouse handling
		addMouseMoveListener(ButtonMouseListener)
		addMouseButtonListener(ButtonMouseListener)
		
		// Adds key listening
		addKeyStateListener(ButtonKeyListener)
		if (hotKeys.nonEmpty || hotKeyChars.nonEmpty)
			addKeyStateListener(new HotKeyListener(hotKeys, hotKeyChars))
		
		// Adds focus listening
		component.addFocusListener(new ButtonFocusListener())
	}
	
	
	// NESTED CLASSES	--------------
	
	private class ButtonFocusListener extends FocusListener
	{
		override def focusGained(e: FocusEvent) = updateFocus(newFocusState = true)
		override def focusLost(e: FocusEvent) = updateFocus(newFocusState = false)
	}
	
	private object ButtonKeyListener extends KeyStateListener
	{
		// ATTRIBUTES   -------------
		
		// Only accepts enter & space presses
		override val keyStateEventFilter = KeyStateEvent.keysFilter(defaultTriggerKeys)
		
		
		// IMPLEMENTED  -------------
		
		override def onKeyState(event: KeyStateEvent) =
		{
			if (event.isDown)
				down = true
			else if (down)
			{
				trigger()
				down = false
			}
		}
		
		// Only allows handling while in focus
		override def allowsHandlingFrom(handlerType: HandlerType) = isInFocus && enabled
	}
	
	private class HotKeyListener(hotKeys: Set[Int], hotKeyCharacters: Iterable[Char]) extends KeyStateListener
	{
		override val keyStateEventFilter =
		{
			if (hotKeys.isEmpty)
				KeyStateEvent.charsFilter(hotKeyCharacters)
			else if (hotKeyCharacters.isEmpty)
				KeyStateEvent.keysFilter(hotKeys)
			else
				KeyStateEvent.keysFilter(hotKeys) || KeyStateEvent.charsFilter(hotKeyCharacters)
		}
		
		override def onKeyState(event: KeyStateEvent) =
		{
			if (event.isDown)
				down = true
			else if (down)
			{
				trigger()
				down = false
			}
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = isEnabled
	}
	
	private object ButtonMouseListener extends MouseButtonStateListener with MouseMoveListener
	{
		// IMPLEMENTED	--------------
		
		// Only listens to left mouse button presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.buttonFilter(MouseButton.Left)
		
		// Listens to mouse enters & exits
		override def mouseMoveEventFilter = MouseMoveEvent.enterAreaFilter(bounds) ||
			MouseMoveEvent.exitedAreaFilter(bounds)
		
		// On left mouse within bounds, brightens color and remembers, on release, returns
		override def onMouseButtonState(event: MouseButtonStateEvent) =
		{
			if (down)
			{
				if (event.wasReleased)
				{
					trigger()
					down = false
					Some(ConsumeEvent("Button released"))
				}
				else
					None
			}
			else if (event.isOverArea(bounds))
			{
				down = true
				Some(ConsumeEvent("Button pressed"))
			}
			else
				None
		}
		
		// When mouse enters, brightens, when mouse leaves, returns
		override def onMouseMove(event: MouseMoveEvent) =
		{
			if (event.isOverArea(bounds))
				state = state.copy(isMouseOver = true)
			else
				state = state.copy(isMouseOver = false)
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = isEnabled
	}
}
