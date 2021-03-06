package utopia.reach.component.template

import java.awt.event.KeyEvent
import utopia.flow.datastructure.mutable.Settable
import utopia.flow.event.ChangingLike
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButton, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.inception.handling.HandlerType
import utopia.reach.cursor.CursorType.{Default, Interactive}
import utopia.reach.focus.{FocusChangeEvent, FocusChangeListener}
import utopia.reach.util.Priority.High
import utopia.reflection.event.{ButtonState, HotKey}

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
  * @since 24.10.2020, v2
  */
trait ButtonLike extends ReachComponentLike with FocusableWithState with CursorDefining
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return The current state of this button
	  */
	def statePointer: ChangingLike[ButtonState]
	
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
	def enabled = state.isEnabled
	
	/**
	  * @return Whether The mouse is currently over this button
	  */
	def isMouseOver = state.isMouseOver
	
	/**
	  * @return Whether this button is currently being pressed down
	  */
	def isPressed = state.isPressed
	
	
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
	override def hasFocus = state.isInFocus
	
	
	// OTHER	------------------------------
	
	/**
	  * Sets up basic event handling in this button. Please note that the <b>focus listening must be set up
	  * separately</b>, since this trait doesn't have access to the subclasses list of listeners.
	  * @param statePointer A mutable pointer to this button's state
	  * @param hotKeys Keys used for triggering this button even while it doesn't have focus (default = empty)
	  * @param triggerKeys Keys used for triggering this button while it has focus (default = space & enter)
	  */
	protected def setup(statePointer: Settable[ButtonState], hotKeys: Set[HotKey] = Set(),
						triggerKeys: Set[Int] = ButtonLike.defaultTriggerKeys) =
	{
		// When connected to the main hierarchy, enables focus management and key listening
		val triggerKeyListener =
		{
			if (triggerKeys.nonEmpty)
				Some(new ButtonKeyListener(statePointer, triggerKeys.map(HotKey.keyWithIndex)))
			else
				None
		}
		val hotKeyListener =
		{
			if (hotKeys.nonEmpty)
				Some(new ButtonKeyListener(statePointer, hotKeys, requiresFocus = false))
			else
				None
		}
		addHierarchyListener { isLinked =>
			if (isLinked)
			{
				triggerKeyListener.foreach(parentHierarchy.top.addKeyStateListener)
				hotKeyListener.foreach(parentHierarchy.top.addKeyStateListener)
				enableFocusHandling()
				parentCanvas.cursorManager.foreach { _ += this }
			}
			else
			{
				triggerKeyListener.foreach(parentHierarchy.top.removeListener)
				hotKeyListener.foreach(parentHierarchy.top.removeListener)
				disableFocusHandling()
				parentCanvas.cursorManager.foreach { _ -= this }
			}
		}
		
		// Starts listening to mouse events as well
		val mouseListener = new ButtonMouseListener(statePointer)
		addMouseButtonListener(mouseListener)
		addMouseMoveListener(mouseListener)
		
		// Repaints this button whenever it changes
		this.statePointer.addListener { _ => repaint(High) }
	}
	
	
	// NESTED	------------------------------
	
	/**
	  * A listener used for updating this button's focus state
	  * @param statePointer A pointer for updating this button's state
	  */
	protected class ButtonDefaultFocusListener(statePointer: Settable[ButtonState]) extends FocusChangeListener
	{
		override def onFocusChangeEvent(event: FocusChangeEvent) =
			statePointer.update { _.copy(isInFocus = event.hasFocus) }
	}
	
	private class ButtonKeyListener(statePointer: Settable[ButtonState], hotKeys: Set[HotKey],
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
		
		def down = statePointer.value.isPressed
		def down_=(newState: Boolean) = statePointer.update { _.copy(isPressed = newState) }
		
		
		// IMPLEMENTED	---------------------------
		
		override def onKeyState(event: KeyStateEvent) =
		{
			if (hotKeys.exists { _.isTriggeredWith(event.keyStatus) })
				down = true
			else if (down)
			{
				trigger()
				down = false
			}
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled && (!requiresFocus || hasFocus)
	}
	
	private class ButtonMouseListener(statePointer: Settable[ButtonState]) extends MouseButtonStateListener
		with MouseMoveListener
	{
		// ATTRIBUTES	----------------------------
		
		// Only listens to left mouse button presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.buttonFilter(MouseButton.Left)
		
		// Listens to mouse enters & exits
		override val mouseMoveEventFilter = MouseMoveEvent.enterAreaFilter(bounds) ||
			MouseMoveEvent.exitedAreaFilter(bounds)
		
		
		// COMPUTED	-------------------------------
		
		def down = statePointer.value.isPressed
		def down_=(newState: Boolean) = statePointer.update { _.copy(isPressed = newState) }
		
		
		// IMPLEMENTED	----------------------------
		
		// On left mouse within bounds, brightens color, gains focus and remembers, on release, returns
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
			else if (event.wasPressed && event.isOverArea(bounds))
			{
				down = true
				if (!hasFocus)
					requestFocus()
				Some(ConsumeEvent("Button pressed"))
			}
			else
				None
		}
		
		// When mouse enters, brightens, when mouse leaves, returns
		override def onMouseMove(event: MouseMoveEvent) =
		{
			if (event.isOverArea(bounds))
				statePointer.update { _.copy(isMouseOver = true) }
			else
				statePointer.update { _.copy(isMouseOver = false) }
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled
	}
}
