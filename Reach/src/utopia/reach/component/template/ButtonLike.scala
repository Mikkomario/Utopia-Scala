package utopia.reach.component.template

import utopia.firmament.model.enumeration.GuiElementState.{Activated, Focused, Hover}
import utopia.firmament.model.{GuiElementStatus, HotKey}
import utopia.flow.util.NotEmpty
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.graphics.Priority.High
import utopia.genesis.handling.event.consume.ConsumeChoice.{Consume, Preserve}
import utopia.genesis.handling.event.keyboard.Key.{Enter, Space}
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseMoveEvent, MouseMoveListener}
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reach.cursor.CursorType
import utopia.reach.cursor.CursorType.{Default, Interactive}
import utopia.reach.focus.{FocusChangeEvent, FocusChangeListener}

object ButtonLike
{
	/**
	  * The default keys used for triggering buttons when they have focus
	  */
	val defaultTriggerKeys = Set[Key](Enter, Space)
}

/**
  * A common trait for Reach button implementations (and wrappers)
  * @author Mikko Hilpinen
  * @since 24.10.2020, v0.1
  */
trait ButtonLike extends ReachComponent with FocusableWithState with CursorDefining with HasGuiState
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return The current state of this button
	  */
	def statePointer: Changing[GuiElementStatus]
	/**
	  * @return A pointer that contains true while this button may be interacted with
	  */
	def enabledPointer: Flag
	
	/**
	  * Triggers the actions associated with this button
	  */
	protected def trigger(): Unit
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return Whether this button is currently being pressed down
	  */
	def isPressed = state is Activated
	
	
	// IMPLEMENTED	--------------------------
	
	/**
	  * @return This button's current state
	  */
	override def state = statePointer.value
	
	override def cursorType: CursorType = if (enabled) Interactive else Default
	
	override def cursorBounds = boundsInsideTop
	
	// By default, buttons always allow focus enter as long as they're enabled
	override def allowsFocusEnter = enabled
	
	// By default, buttons always allow focus leave
	override def allowsFocusLeave = true
	
	
	// OTHER	------------------------------
	
	/**
	  * Sets up basic event handling in this button. Please note that the <b>focus listening must be set up
	  * separately</b>, since this trait doesn't have access to the subclasses list of listeners.
	  * @param statePointer A mutable pointer to this button's state
	  * @param hotKeys Keys used for triggering this button even while it doesn't have focus (default = empty)
	  * @param triggerKeys Keys used for triggering this button while it has focus (default = space & enter)
	  */
	protected def setup(statePointer: Pointer[GuiElementStatus], hotKeys: Set[HotKey] = Set(),
	                    triggerKeys: Set[Key] = ButtonLike.defaultTriggerKeys) =
	{
		// When connected to the main hierarchy, enables focus management and key listening
		val triggerKeyListener = NotEmpty(triggerKeys).map { keys =>
			new ButtonKeyListener(statePointer, keys.map(HotKey.apply))
		}
		val hotKeyListener = NotEmpty(hotKeys).map { keys =>
			new ButtonKeyListener(statePointer, keys, requiresFocus = false)
		}
		addHierarchyListener { isLinked =>
			if (isLinked) {
				triggerKeyListener.foreach { KeyboardEvents += _ }
				hotKeyListener.foreach { KeyboardEvents += _ }
				enableFocusHandling()
				parentCanvas.cursorManager.foreach { _ += this }
			}
			else {
				triggerKeyListener.foreach { KeyboardEvents -= _ }
				hotKeyListener.foreach { KeyboardEvents -= _ }
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
		
		// Listens to keys involved in the hotkeys. Doesn't necessarily mean that any key is triggered.
		override val keyStateEventFilter = KeyStateEventFilter(hotKeys.flatMap { _.keys })
		
		override val handleCondition: Flag = {
			if (requiresFocus)
				enabledPointer && focusPointer
			else
				enabledPointer
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
				key.isTriggeredWith(event.keyboardState) && (key.triggersWithoutWindowFocus || windowHasFocus) })
				down = true
			else if (down) {
				trigger()
				down = false
			}
		}
	}
	
	private class ButtonMouseListener(statePointer: Pointer[GuiElementStatus])
		extends MouseButtonStateListener with MouseMoveListener
	{
		// ATTRIBUTES	----------------------------
		
		// Only listens to left mouse button presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.filter.left
		// Listens to mouse enters & exits
		override val mouseMoveEventFilter = MouseMoveEvent.filter.enteredOrExited(bounds)
		
		
		// COMPUTED	-------------------------------
		
		def down = statePointer.value is Activated
		def down_=(newState: Boolean) = statePointer.update { _ + (Activated -> newState) }
		
		
		// IMPLEMENTED	----------------------------
		
		override def handleCondition: Flag = enabledPointer
		
		// On left mouse within bounds, brightens color, gains focus and remembers, on release, returns
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
			if (down) {
				if (event.released) {
					trigger()
					down = false
					Consume("Button released")
				}
				else
					Preserve
			}
			else if (event.pressed && event.isOver(bounds)) {
				down = true
				if (!hasFocus)
					requestFocus()
				Consume("Button pressed")
			}
			else
				Preserve
		}
		
		// When mouse enters, brightens, when mouse leaves, returns
		override def onMouseMove(event: MouseMoveEvent) = {
			if (event.isOver(bounds))
				statePointer.update { _ + Hover }
			else
				statePointer.update { _ - Hover }
		}
	}
}
