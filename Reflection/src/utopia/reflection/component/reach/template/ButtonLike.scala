package utopia.reflection.component.reach.template

import java.awt.event.KeyEvent
import utopia.flow.datastructure.mutable.Settable
import utopia.flow.event.ChangingLike
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButton, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.inception.handling.HandlerType
import utopia.reflection.cursor.CursorType.{Default, Interactive}
import utopia.reflection.event.{ButtonState, FocusChangeEvent, FocusChangeListener}
import utopia.reflection.util.Priority.High

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
trait ButtonLike extends ReachComponentLike with Focusable with CursorDefining
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
	  * @return Whether this button currently has focus
	  */
	def hasFocus = state.isInFocus
	
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
	
	
	// OTHER	------------------------------
	
	/**
	  * Sets up basic event handling in this button. Please note that the <b>focus listening must be set up
	  * separately</b>, since this trait doesn't have access to the subclasses list of listeners.
	  * @param statePointer A mutable pointer to this button's state
	  * @param hotKeys Keys used for triggering this button even while it doesn't have focus (default = empty)
	  * @param hotKeyCharacters Character keys used for triggering this button even while it doesn't have focus
	  *                         (default = empty)
	  * @param triggerKeys Keys used for triggering this button while it has focus (default = space & enter)
	  */
	protected def setup(statePointer: Settable[ButtonState], hotKeys: Set[Int] = Set(),
						hotKeyCharacters: Iterable[Char] = Set(), triggerKeys: Set[Int] = ButtonLike.defaultTriggerKeys) =
	{
		// When connected to the main hierarchy, enables focus management and key listening
		val triggerKeyListener =
		{
			if (triggerKeys.nonEmpty)
				Some(new ButtonKeyListener(statePointer, triggerKeys))
			else
				None
		}
		val hotKeyListener =
		{
			if (hotKeys.nonEmpty || hotKeyCharacters.nonEmpty)
				Some(new ButtonKeyListener(statePointer, hotKeys, hotKeyCharacters, requiresFocus = false))
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
	
	private class ButtonKeyListener(statePointer: Settable[ButtonState], triggerKeys: Set[Int] = Set(),
								 triggerCharacters: Iterable[Char] = Set(), requiresFocus: Boolean = true)
		extends KeyStateListener
	{
		// ATTRIBUTES	---------------------------
		
		override val keyStateEventFilter =
		{
			if (triggerKeys.isEmpty)
				KeyStateEvent.charsFilter(triggerCharacters)
			else if (triggerCharacters.isEmpty)
				KeyStateEvent.keysFilter(triggerKeys)
			else
				KeyStateEvent.keysFilter(triggerKeys) || KeyStateEvent.charsFilter(triggerCharacters)
		}
		
		
		// COMPUTED	-------------------------------
		
		def down = statePointer.value.isPressed
		def down_=(newState: Boolean) = statePointer.update { _.copy(isPressed = newState) }
		
		
		// IMPLEMENTED	---------------------------
		
		override def onKeyState(event: KeyStateEvent) =
		{
			if (triggerKeys.exists { event.keyStatus(_) } || triggerCharacters.exists { event.keyStatus(_) })
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
