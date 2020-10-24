package utopia.reflection.component.reach.template

import java.awt.event.KeyEvent

import utopia.flow.datastructure.mutable.PointerLike
import utopia.genesis.event.{ConsumeEvent, KeyStateEvent, MouseButton, MouseButtonStateEvent, MouseMoveEvent}
import utopia.genesis.handling.{KeyStateListener, MouseButtonStateListener, MouseMoveListener}
import utopia.inception.handling.HandlerType
import utopia.reflection.event.{ButtonState, FocusChangeEvent, FocusChangeListener}

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
trait ButtonLike extends ReachComponentLike with Focusable
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return The current state of this button
	  */
	def state: ButtonState
	
	/**
	  * Triggers the actions associated with this button
	  */
	protected def trigger(): Unit
	
	
	// COMPUTED	------------------------------
	
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
	
	// By default, buttons always allow focus enter as long as they're enabled
	override def allowsFocusEnter = enabled
	
	// By default, buttons always allow focus leave
	override def allowsFocusLeave = true
	
	
	// OTHER	------------------------------
	
	protected def setup(statePointer: PointerLike[ButtonState], triggerKeys: Set[Int] = ButtonLike.defaultTriggerKeys,
						hotKeys: Set[Int] = Set(), hotKeyCharacters: Iterable[Char] = Set()) =
	{
		// When connected to the main hierarchy, enables focus management and key listening
		// TODO: Implement
	}
	
	
	// NESTED	------------------------------
	
	/**
	  * A listener used for updating this button's focus state
	  * @param statePointer A pointer for updating this button's state
	  */
	protected class ButtonDefaultFocusListener(statePointer: PointerLike[ButtonState]) extends FocusChangeListener
	{
		override def onFocusChangeEvent(event: FocusChangeEvent) =
			statePointer.update { _.copy(isInFocus = event.hasFocus) }
	}
	
	private class ButtonKeyListener(statePointer: PointerLike[ButtonState], triggerKeys: Set[Int],
								 triggerCharacters: Iterable[Char], requiresFocus: Boolean) extends KeyStateListener
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
		
		def down = statePointer.get.isPressed
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
	
	private class ButtonMouseListener(statePointer: PointerLike[ButtonState]) extends MouseButtonStateListener
		with MouseMoveListener
	{
		// ATTRIBUTES	----------------------------
		
		// Only listens to left mouse button presses & releases
		override val mouseButtonStateEventFilter = MouseButtonStateEvent.buttonFilter(MouseButton.Left)
		
		// Listens to mouse enters & exits
		override val mouseMoveEventFilter = MouseMoveEvent.enterAreaFilter(bounds) ||
			MouseMoveEvent.exitedAreaFilter(bounds)
		
		
		// COMPUTED	-------------------------------
		
		def down = statePointer.get.isPressed
		def down_=(newState: Boolean) = statePointer.update { _.copy(isPressed = newState) }
		
		
		// IMPLEMENTED	----------------------------
		
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
				statePointer.update { _.copy(isMouseOver = true) }
			else
				statePointer.update { _.copy(isMouseOver = false) }
		}
		
		override def allowsHandlingFrom(handlerType: HandlerType) = enabled
	}
}
