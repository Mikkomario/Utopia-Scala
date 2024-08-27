package utopia.genesis.handling.event.keyboard

import utopia.flow.async.context.ActionQueue
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.handling.event.keyboard.KeyLocation.Standard
import utopia.genesis.handling.template.{Handleable, Handlers}

import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Common interface for keyboard events throughout the application
  * @author Mikko Hilpinen
  * @since 12.9.2020, v2.4
  */
object KeyboardEvents extends mutable.Growable[Handleable]
{
	// ATTRIBUTES	------------------
	
	private lazy val keyStateHandler = KeyStateHandler()
	// This handler version will only receive events from AWT, not generated events
	private lazy val directKeyTypedHandler = KeyTypedHandler()
	private lazy val keyTypedHandler = {
		val handler = KeyTypedHandler()
		directKeyTypedHandler += handler
		handler
	}
	private lazy val keyDownHandler = KeyDownHandler()
	
	private lazy val handlers = Handlers(Vector(keyStateHandler, keyTypedHandler, keyDownHandler))
	
	private var _state = KeyboardState.default
	private var lastPressedKeyIndex = 0
	private var keyDownStarted = false
	
	// Event queue is used after the execution context has been specified
	private var eventQueue: Option[ActionQueue] = None
	
	
	// INITIAL CODE	-----------------
	
	// Starts listening to dispatched keyboard events
	KeyboardFocusManager.getCurrentKeyboardFocusManager.addKeyEventDispatcher { event: KeyEvent =>
		lazy val index = event.getExtendedKeyCode
		event.getID match {
			case KeyEvent.KEY_PRESSED =>
				lastPressedKeyIndex = index
				stateChanged(event, index, pressed = true)
			case KeyEvent.KEY_RELEASED => stateChanged(event, index, pressed = false)
			case KeyEvent.KEY_TYPED =>
				if (directKeyTypedHandler.mayBeHandled) {
					val newEvent = KeyTypedEvent(event.getKeyChar, lastPressedKeyIndex, _state)
					// Distributes the event asynchronously, if possible
					queue { directKeyTypedHandler.onKeyTyped(newEvent) }
				}
		}
		false
	}
	
	
	// COMPUTED	----------------------
	
	/**
	  * @return The current keyboard state, showing which keys are currently pressed
	  */
	def state = _state
	/**
	  * @return The current key status
	  */
	@deprecated("Please use .state instead", "v4.0")
	def keyStatus = state
	
	
	// IMPLEMENTED  -----------------
	
	override def addOne(elem: Handleable) = {
		handlers += elem
		this
	}
	
	override def clear() = handlers.clear()
	
	
	// OTHER	--------------------
	
	/**
	  * Sets up the execution context that is used for distributing keyboard events
	  * @param context An execution context used when distributing keyboard events
	  */
	def specifyExecutionContext(context: ExecutionContext)(implicit log: Logger) =
		eventQueue = Some(new ActionQueue()(context, log))
	
	/**
	 * Sets up the key-down event generation, unless it has been set up already.
	 * Also sets up auto-generated continuous key-typed events while holding down a key (optional feature)
	 * @param actorHandler An actor handler that will deliver action events required for event-generation
	 * @param beforeMultiTypeDelay Duration how long a key must be held down before
	 *                             continuous key typed -events will be generated.
	 *                             Set to infinite in order to disable continuous key typed -events.
	 *                             Default = 0.8 seconds.
	 * @param multiTypeInterval Time interval between generated key typed -events. Default = 0.2 seconds
	  * @param log Implicit logging implementation used in pointer-related error-handling
	 */
	def setupKeyDownEvents(actorHandler: ActorHandler, beforeMultiTypeDelay: Duration = 0.8.seconds,
	                       multiTypeInterval: FiniteDuration = 0.2.seconds)
	                      (implicit log: Logger): Unit =
	{
		if (!keyDownStarted) {
			keyDownStarted = true
			
			// Sets up the key down -events
			val generator = new KeyDownEventGenerator(keyDownHandler)
			generator.start(actorHandler, keyStateHandler)
			
			// Also starts key typed -event generation
			beforeMultiTypeDelay.finite.foreach { delay =>
				multiTypeInterval.finite.foreach { interval =>
					HoldKeyToTypeGenerator.start(keyDownHandler, keyStateHandler, directKeyTypedHandler,
						keyTypedHandler, delay, interval)
				}
			}
		}
	}
	
	/**
	  * Adds a new keyboard state listener
	  * @param listener A new keyboard state listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerKeyStateListener(listener: KeyStateListener) =
		keyStateHandler += listener
	/**
	  * Adds a new key typed listener
	  * @param listener A new key typed listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def registerKeyTypedListener(listener: KeyTypedListener) =
		directKeyTypedHandler += listener
	/**
	  * Adds a new keyboard related listener. If the listener is not a KeyStateListener nor a KeyTypedListener,
	  * no action is taken
	  * @param listener A new listener
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def register(listener: Handleable) = handlers += listener
	/**
	  * Removes a listener from receiving any more events
	  * @param listener A listener that doesn't need to receive events any more
	  */
	@deprecated("Deprecated for removal", "v4.0")
	def unregister(listener: Handleable) = handlers -= listener
	
	/**
	  * Removes a listener from receiving any more events
	  * @param listener A listener that doesn't need to receive events any more
	  */
	def -=(listener: Handleable) = handlers -= listener
	/**
	  * Removes 0-n listeners from receiving any more events
	  * @param listeners Listeners to remove / detach
	  */
	def --=(listeners: IterableOnce[Handleable]) = handlers --= listeners
	
	private def stateChanged(event: KeyEvent, index: Int, pressed: Boolean) = {
		val location = KeyLocation.of(event.getKeyLocation).getOrElse(Standard)
		// Only reacts to status changes
		if (_state(index, location) != pressed) {
			_state = _state.withKeyState(index, location, pressed)
			if (keyStateHandler.mayBeHandled) {
				val newEvent = KeyStateEvent(index, location, _state, pressed)
				// Distributes the event asynchronously, if possible
				queue { keyStateHandler.onKeyState(newEvent) }
			}
		}
	}
	
	private def queue(action: => Unit): Unit = eventQueue match {
		case Some(queue) => queue.push(action)
		case None => action
	}
}
