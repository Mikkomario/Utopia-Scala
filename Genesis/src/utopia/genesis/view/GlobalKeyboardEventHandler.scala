package utopia.genesis.view

import java.awt.event.KeyEvent
import java.awt.KeyboardFocusManager

import utopia.genesis.event.KeyLocation.Standard
import utopia.genesis.event.{KeyLocation, KeyStateEvent, KeyStatus, KeyTypedEvent}
import utopia.genesis.handling.{KeyStateListener, KeyTypedListener}
import utopia.genesis.handling.mutable
import utopia.inception.handling.Handleable
import utopia.inception.handling.mutable.HandlerRelay

/**
  * This key listener converts java.awt.KeyEvents to various Utopia Genesis key events
  * @author Mikko Hilpinen
  * @since 12.9.2020, v2.4
  */
object GlobalKeyboardEventHandler
{
	// ATTRIBUTES	------------------
	
	private lazy val keyStateHandler = mutable.KeyStateHandler()
	private lazy val keyTypedHandler = mutable.KeyTypedHandler()
	
	private lazy val handlers = HandlerRelay(keyStateHandler, keyTypedHandler)
	
	private var _keyStatus = KeyStatus.empty
	
	
	// INITIAL CODE	-----------------
	
	// Starts listening to dispatched keyboard events
	KeyboardFocusManager.getCurrentKeyboardFocusManager.addKeyEventDispatcher((e: KeyEvent) => {
		if (e.getID == KeyEvent.KEY_PRESSED)
			keyStateChanged(e, newState = true)
		else if (e.getID == KeyEvent.KEY_RELEASED)
			keyStateChanged(e, newState = false)
		else if (e.getID == KeyEvent.KEY_TYPED)
			keyTypedHandler.onKeyTyped(KeyTypedEvent(e.getKeyChar, _keyStatus))
		
		false
	})
	
	
	// COMPUTED	----------------------
	
	/**
	  * @return The current key status
	  */
	def keyStatus = _keyStatus
	
	/**
	  * @return A string representation of the currently handled listeners
	  */
	def debugString = handlers.debugString
	
	
	// OTHER	--------------------
	
	/**
	  * Adds a new keyboard state listener
	  * @param listener A new keyboard state listener
	  */
	def registerKeyStateListener(listener: KeyStateListener) = keyStateHandler += listener
	
	/**
	  * Adds a new key typed listener
	  * @param listener A new key typed listener
	  */
	def registerKeyTypedListener(listener: KeyTypedListener) = keyTypedHandler += listener
	
	/**
	  * Adds a new keyboard related listener. If the listener is not a KeyStateListener nor a KeyTypedListener,
	  * no action is taken
	  * @param listener A new listener
	  */
	def register(listener: Handleable) = handlers += listener
	
	/**
	  * Removes a listener from receiving any more events
	  * @param listener A listener that doesn't need to receive events any more
	  */
	def unregister(listener: Handleable) = handlers -= listener
	
	/**
	  * Adds a new keyboard related listener. If the listener is not a KeyStateListener nor a KeyTypedListener,
	  * no action is taken
	  * @param listener A new listener
	  */
	def +=(listener: Handleable) = register(listener)
	
	/**
	  * Removes a listener from receiving any more events
	  * @param listener A listener that doesn't need to receive events any more
	  */
	def -=(listener: Handleable) = unregister(listener)
	
	private def keyStateChanged(e: KeyEvent, newState: Boolean) =
	{
		val location = KeyLocation.of(e.getKeyLocation).getOrElse(Standard)
		
		// Only reacts to status changes
		if (_keyStatus(e.getExtendedKeyCode, location) != newState)
		{
			_keyStatus += (e.getExtendedKeyCode, location, newState)
			keyStateHandler.onKeyState(new KeyStateEvent(e.getExtendedKeyCode, location, newState, _keyStatus))
		}
	}
}
