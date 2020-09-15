package utopia.reflection.component.swing.button

import java.awt.event.{FocusEvent, FocusListener, KeyEvent}

import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButton, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.inception.handling.HandlerType
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.{ComponentLike, Focusable}

/**
  * Used as a common trait for all different button implementations
  * @author Mikko Hilpinen
  * @since 25.4.2019, v1+
  */
// TODO: Add support for hotkeys
trait ButtonLike extends ComponentLike with AwtComponentRelated with Focusable
{
	// ABSTRACT	----------------------
	
	/**
	  * Updates this button's style to match the new state
	  * @param newState New button state
	  */
	protected def updateStyleForState(newState: ButtonState): Unit
	
	
	// ATTRIBUTES	------------------
	
	private var actions = Vector[() => Unit]()
	private var _state: ButtonState = ButtonState(isEnabled = true, isInFocus = false, isMouseOver = false, isPressed = false)
	
	
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
	
	
	// IMPLEMENTED	------------------
	
	override def requestFocusInWindow() = component.requestFocusInWindow()
	
	override def isInFocus = state.isInFocus
	private def isInFocus_=(newFocusState: Boolean) = state = state.copy(isInFocus = newFocusState)
	
	
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
	protected def initializeListeners() =
	{
		// Adds mouse handling
		addMouseMoveListener(ButtonMouseListener)
		addMouseButtonListener(ButtonMouseListener)
		
		// Adds key listening
		addKeyStateListener(ButtonKeyListener)
		
		// Adds focus listening
		component.addFocusListener(new ButtonFocusListener())
	}
	
	
	// NESTED CLASSES	--------------
	
	private class ButtonFocusListener extends FocusListener
	{
		override def focusGained(e: FocusEvent) =
		{
			if (!isInFocus)
				isInFocus = true
		}
		
		override def focusLost(e: FocusEvent) =
		{
			if (isInFocus)
				isInFocus = false
		}
	}
	
	private object ButtonKeyListener extends KeyStateListener
	{
		// Only accepts enter & space presses
		override val keyStateEventFilter = KeyStateEvent.wasPressedFilter &&
			(KeyStateEvent.keyFilter(KeyEvent.VK_SPACE) || KeyStateEvent.keyFilter(KeyEvent.VK_ENTER))
		
		override def onKeyState(event: KeyStateEvent) = trigger()
		
		// Only allows handling while in focus
		override def allowsHandlingFrom(handlerType: HandlerType) = isInFocus
	}
	
	private object ButtonMouseListener extends MouseButtonStateListener with MouseMoveListener
	{
		// ATTRIBUTES	--------------
		
		def down = state.isPressed
		def down_=(newState: Boolean) = state = state.copy(isPressed = newState)
		
		
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
