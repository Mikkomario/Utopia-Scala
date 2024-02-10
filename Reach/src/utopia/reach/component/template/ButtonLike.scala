package utopia.reach.component.template

import utopia.firmament.model.enumeration.GuiElementState.{Activated, Disabled, Focused, Hover}
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.util.NotEmpty
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButton, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.cursor.CursorType.{Default, Interactive}
import utopia.reach.focus.{FocusChangeEvent, FocusChangeListener}
import utopia.genesis.graphics.Priority2.High

import java.awt.event.KeyEvent

object ButtonLike
{
	/**
	  * The default keys used for triggering buttons when they have focus
	  */
	val defaultTriggerKeys = Set(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE)
}

/**
  * A common trait for Reach button implementations (and wrappers)
  * @author Mikko Hilpinen
  * @since 24.10.2020, v0.1
  */
trait ButtonLike extends ReachComponentLike with FocusableWithState with CursorDefining
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return The current state of this button
	  */
	def statePointer: Changing[GuiElementStatus]
	
	/**
	  * Triggers the actions associated with this button
	  */
	protected def trigger(): Unit
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return This button's current state
	  */
	def state = statePointer.value
	
	/**
	  * @return Whether this button is currently enabled
	  */
	def enabled = state isNot Disabled
	/**
	  * @return Whether The mouse is currently over this button
	  */
	def isMouseOver = state is Hover
	/**
	  * @return Whether this button is currently being pressed down
	  */
	def isPressed = state is Activated
	
	
	// IMPLEMENTED	--------------------------
	
	override def cursorType = if (enabled) Interactive else Default
	
	override def cursorBounds = boundsInsideTop
	
	// By default, buttons always allow focus enter as long as they're enabled
	override def allowsFocusEnter = enabled
	
	// By default, buttons always allow focus leave
	override def allowsFocusLeave = true
	
	/**
	  * @return Whether this button currently has focus
	  */
	override def hasFocus = state is Focused
	
	
	// OTHER	------------------------------
	
	/**
	  * Sets up basic event handling in this button. Please note that the <b>focus listening must be set up
	  * separately</b>, since this trait doesn't have access to the subclasses list of listeners.
	  * @param statePointer A mutable pointer to this button's state
	  * @param hotKeys Keys used for triggering this button even while it doesn't have focus (default = empty)
	  * @param triggerKeys Keys used for triggering this button while it has focus (default = space & enter)
	  */
	protected def setup(statePointer: Pointer[GuiElementStatus], hotKeys: Set[HotKey] = Set(),
	                    triggerKeys: Set[Int] = ButtonLike.defaultTriggerKeys) =
	{
		// When connected to the main hierarchy, enables focus management and key listening
		val triggerKeyListener = NotEmpty(triggerKeys).map { keys =>
			new ButtonKeyListener(statePointer, keys.map(HotKey.keyWithIndex))
		}
		val hotKeyListener = NotEmpty(hotKeys).map { keys =>
			new ButtonKeyListener(statePointer, keys, requiresFocus = false)
		}
		addHierarchyListener { isLinked =>
			if (isLinked) {
				triggerKeyListener.foreach { GlobalKeyboardEventHandler += _ }
				hotKeyListener.foreach { GlobalKeyboardEventHandler += _ }
				enableFocusHandling()
				parentCanvas.cursorManager.foreach { _ += this }
			}
			else {
				triggerKeyListener.foreach { GlobalKeyboardEventHandler -= _ }
				hotKeyListener.foreach { GlobalKeyboardEventHandler -= _ }
				disableFocusHandling()
				parentCanvas.cursorManager.foreach { _ -= this }
			}
		}
		
		// Starts listening to mouse events as well
		val mouseListener = new ButtonMouseListener(statePointer)
		addMouseButtonListener(mouseListener)
		addMouseMoveListener(mouseListener)
		
		// Repaints this button whenever it changes
		this.statePointer.addContinuousAnyChangeListener { repaint(High) }
	}
	
	
	// NESTED	------------------------------
	
	/**
	  * A listener used for updating this button's focus state
	  * @param statePointer A pointer for updating this button's state
	  */
	protected class ButtonDefaultFocusListener(statePointer: Pointer[GuiElementStatus]) extends FocusChangeListener
	{
		override def onFocusChangeEvent(event: FocusChangeEvent) =
			statePointer.update { state =>
				if (event.hasFocus)
					state + Focused
				else
					state - Focused
			}
	}
	
	private class ButtonKeyListener(statePointer: Pointer[GuiElementStatus], hotKeys: Set[HotKey],
	                                requiresFocus: Boolean = true)
		extends KeyStateListener
	{
		// ATTRIBUTES	---------------------------
		
		override val keyStateEventFilter =
		{
			val allIndices = hotKeys.flatMap { _.keyIndices }
			val allChars = hotKeys.flatMap { _.characters }
			
			if (allIndices.isEmpty)
				KeyStateEvent.charsFilter(allChars)
			else if (allChars.isEmpty)
				KeyStateEvent.keysFilter(allIndices)
			else
				KeyStateEvent.keysFilter(allIndices) || KeyStateEvent.charsFilter(allChars)
		}
		
		
		// COMPUTED	-------------------------------
		
		def down = statePointer.value is Activated
		def down_=(newState: Boolean) = statePointer.update { state =>
			if (newState)
				state + Activated
			else
				state - Activated
		}
		
		
		// IMPLEMENTED	---------------------------
		
		override def onKeyState(event: KeyStateEvent) = {
			lazy val windowHasFocus = parentWindow.exists { window => window.isFocused || !window.isFocusableWindow }
			if (hotKeys.exists { key =>
				key.isTriggeredWith(event.keyStatus) && (key.triggersWithoutWindowFocus || windowHasFocus) })
				down = true
			else if (down) {
				trigger()
				down = false
			}
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled && (!requiresFocus || hasFocus)
	}
	
	private class ButtonMouseListener(statePointer: Pointer[GuiElementStatus]) extends MouseButtonStateListener
		with MouseMoveListener
	{
		// ATTRIBUTES	----------------------------
		
		// Only listens to left mouse button presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.buttonFilter(MouseButton.Left)
		
		// Listens to mouse enters & exits
		override val mouseMoveEventFilter = MouseMoveEvent.enteredOrExitedAreaFilter(bounds)
		
		
		// COMPUTED	-------------------------------
		
		def down = statePointer.value is Activated
		def down_=(newState: Boolean) = statePointer.update { _ + (Activated -> newState) }
		
		
		// IMPLEMENTED	----------------------------
		
		// On left mouse within bounds, brightens color, gains focus and remembers, on release, returns
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
			else if (event.wasPressed && event.isOverArea(bounds)) {
				down = true
				if (!hasFocus)
					requestFocus()
				Some(ConsumeEvent("Button pressed"))
			}
			else
				None
		}
		
		// When mouse enters, brightens, when mouse leaves, returns
		override def onMouseMove(event: MouseMoveEvent) = {
			if (event.isOverArea(bounds))
				statePointer.update { _ + Hover }
			else
				statePointer.update { _ - Hover }
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled
	}
}
