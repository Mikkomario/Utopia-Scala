package utopia.reflection.component.swing.button

import utopia.firmament.model.GuiElementStatus
import utopia.firmament.model.enumeration.GuiElementState.{Activated, Disabled, Focused, Hover}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key.{Enter, Space}
import utopia.genesis.handling.event.keyboard.KeyStateListener.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent2, MouseButtonStateListener2, MouseMoveEvent2, MouseMoveListener2}
import utopia.reflection.component.swing.template.AwtComponentRelated
import utopia.reflection.component.template.Focusable
import utopia.reflection.component.template.layout.stack.ReflectionStackable

import java.awt.event.{FocusEvent, FocusListener}

object ButtonLike
{
	private val defaultTriggerKeys = Set[Key](Enter, Space)
}

/**
  * Used as a common trait for all different button implementations
  * @author Mikko Hilpinen
  * @since 25.4.2019, v1+
  */
// TODO: Should be an abstract class (because contains concrete properties)
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
	protected def initializeListeners(hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set()) = {
		// Adds mouse handling
		addMouseMoveListener(ButtonMouseListener)
		addMouseButtonListener(ButtonMouseListener)
		
		// Adds key listening
		lazy val hotKeyListener = new HotKeyListener(hotKeys, hotKeyChars)
		addStackHierarchyChangeListener(attached => {
			if (attached) {
				KeyboardEvents += ButtonKeyListener
				if (hotKeys.nonEmpty || hotKeyChars.nonEmpty)
					KeyboardEvents += hotKeyListener
			}
			else {
				KeyboardEvents -= ButtonKeyListener
				KeyboardEvents -= hotKeyListener
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
		override val keyStateEventFilter = KeyStateEvent.filter(defaultTriggerKeys)
		
		
		// IMPLEMENTED  -------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def onKeyState(event: KeyStateEvent) = {
			// Only allows handling while in focus
			if (isInFocus && enabled) {
				if (event.pressed)
					down = true
				else if (down) {
					trigger()
					down = false
				}
			}
		}
	}
	
	private class HotKeyListener(hotKeys: Set[Int], hotKeyCharacters: Iterable[Char]) extends KeyStateListener
	{
		override val keyStateEventFilter =
			KeyStateEventFilter { e => hotKeys.contains(e.index) || hotKeyCharacters.exists(e.concernsChar) }
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def onKeyState(event: KeyStateEvent) = {
			if (enabled) {
				if (event.pressed)
					down = true
				else if (down) {
					trigger()
					down = false
				}
			}
		}
	}
	
	private object ButtonMouseListener extends MouseButtonStateListener2 with MouseMoveListener2
	{
		// ATTRIBUTES   ---------------
		
		// Only listens to left mouse button presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent2.filter.left
		// Listens to mouse enters & exits
		override val mouseMoveEventFilter = MouseMoveEvent2.filter.enteredOrExited(bounds)
		
		
		// IMPLEMENTED	--------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		// On left mouse within bounds, brightens color and remembers, on release, returns
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent2) = {
			if (enabled) {
				if (down) {
					if (event.released) {
						trigger()
						down = false
						Consume("Button released")
					}
					else
						Preserve
				}
				else if (event.isOver(bounds)) {
					down = true
					Consume("Button pressed")
				}
				else
					Preserve
			}
			else
				Preserve
		}
		
		// When mouse enters, brightens, when mouse leaves, returns
		override def onMouseMove(event: MouseMoveEvent2) = {
			if (enabled) {
				if (event.isOver(bounds))
					state += Hover
				else
					state -= Hover
			}
		}
	}
}
