package utopia.reflection.component.swing.button

import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.{Activated, Disabled, Focused, Hover}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButton, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.Focusable
import utopia.reflection.component.template.layout.stack.ReflectionStackable

import java.awt.event.{FocusEvent, FocusListener, KeyEvent}

object ButtonLike
{
	private val defaultTriggerKeys = Set(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE)
}

/**
  * Used as a common trait for all different button implementations
  * @author Mikko Hilpinen
  * @since 25.4.2019, v1+
  */
trait ButtonLike extends ReflectionStackable with AwtComponentRelated with Focusable
{
	import ButtonLike._
	
	// ABSTRACT	----------------------
	
	/**
	  * Updates this button's style to match the new state
	  * @param newState New button state
	  */
	protected def updateStyleForState(newState: GuiElementStatus): Unit
	
	
	// ATTRIBUTES	------------------
	
	private var actions = Vector[() => Unit]()
	
	private val _statePointer = new EventfulPointer[GuiElementStatus](GuiElementStatus.identity)
	
	
	// COMPUTED	----------------------
	
	/**
	  * @return A read-only pointer to this button's state
	  */
	def statePointer = _statePointer.readOnly
	
	/**
	  * @return Whether this button is currently enabled
	  */
	def enabled = component.isEnabled
	def enabled_=(newEnabled: Boolean) = {
		state -= Disabled
		component.setEnabled(newEnabled)
	}
	
	/**
	  * @return Whether this button is currently enabled
	  */
	@deprecated("Replaced with .enabled", "v2.1.1")
	def isEnabled = enabled
	
	/**
	  * @return This button's current state
	  */
	def state = _statePointer.value
	private def state_=(newState: GuiElementStatus) = {
		if (statePointer.value != newState) {
			_statePointer.value = newState
			updateStyleForState(newState)
		}
	}
	
	private def down = state is Activated
	private def down_=(newState: Boolean) = {
		if (newState)
			state += Activated
		else
			state -= Activated
	}
	
	
	// IMPLEMENTED	------------------
	
	override def requestFocusInWindow() = component.requestFocusInWindow()
	
	override def isInFocus = state is Focused
	private def updateFocus(newFocusState: Boolean) = {
		if (newFocusState)
			state += Focused
		else
			state -= Focused
	}
	
	
	// OTHER	----------------------
	
	/**
	  * Adds a new action to this button. The action will be performed whenever this button is pressed
	  * @param action An action that should be performed when this button is pressed
	  */
	def registerAction(action: () => Unit) = actions :+= action
	
	/**
	  * Triggers this button's action. Same as if the user clicked this button (only works for enabled buttons)
	  */
	def trigger() = if (enabled) actions.foreach { _() }
	
	/**
	  * Initializes this button's listeners. Should be called when constructing this button.
	  */
	protected def initializeListeners(hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set()) =
	{
		// Adds mouse handling
		addMouseMoveListener(ButtonMouseListener)
		addMouseButtonListener(ButtonMouseListener)
		
		// Adds key listening
		lazy val hotKeyListener = new HotKeyListener(hotKeys, hotKeyChars)
		addStackHierarchyChangeListener(attached => {
			if (attached) {
				GlobalKeyboardEventHandler += ButtonKeyListener
				if (hotKeys.nonEmpty || hotKeyChars.nonEmpty)
					GlobalKeyboardEventHandler += hotKeyListener
			}
			else {
				GlobalKeyboardEventHandler -= ButtonKeyListener
				GlobalKeyboardEventHandler -= hotKeyListener
			}
		}, callIfAttached = true)
		
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
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled
	}
	
	private object ButtonMouseListener extends MouseButtonStateListener with MouseMoveListener
	{
		// IMPLEMENTED	--------------
		
		// Only listens to left mouse button presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.buttonFilter(MouseButton.Left)
		
		// Listens to mouse enters & exits
		override val mouseMoveEventFilter = MouseMoveEvent.enterAreaFilter(bounds) ||
			MouseMoveEvent.exitedAreaFilter(bounds)
		
		// On left mouse within bounds, brightens color and remembers, on release, returns
		override def onMouseButtonState(event: MouseButtonStateEvent) = {
			if (down) {
				if (event.wasReleased) {
					trigger()
					down = false
					Some(ConsumeEvent("Button released"))
				}
				else
					None
			}
			else if (event.isOverArea(bounds)) {
				down = true
				Some(ConsumeEvent("Button pressed"))
			}
			else
				None
		}
		
		// When mouse enters, brightens, when mouse leaves, returns
		override def onMouseMove(event: MouseMoveEvent) = {
			if (event.isOverArea(bounds))
				state += Hover
			else
				state -= Hover
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled
	}
}
