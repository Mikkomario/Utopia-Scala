package utopia.genesis.handling.event.keyboard

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.filter.AcceptAll
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.Settable
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.template.Handlers

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object KeyDownEventGenerator
{
	/**
	  * @param actorHandler    An actor handler that will deliver action events to this generator
	  * @param keyStateHandler A key-state event handler that will inform this generator of keyboard state changes
	  * @param activeCondition A condition that must be met in order for key-down events to be generated or fired
	  * @return A new functioning key-down event generator
	  */
	@deprecated("Please use .start(...) instead", "v4.0.1")
	def apply(actorHandler: ActorHandler, keyStateHandler: KeyStateHandler,
	          activeCondition: Flag = AlwaysTrue): KeyDownEventGenerator =
		start(actorHandler, keyStateHandler, activeCondition)(SysErrLogger)
	
	/**
	  * @param actorHandler An actor handler that will deliver action events to this generator
	  * @param keyStateHandler A key-state event handler that will inform this generator of keyboard state changes
	  * @param activeCondition A condition that must be met in order for key-down events to be generated or fired
	  * @return A new started key-down event generator
	  */
	def start(actorHandler: ActorHandler, keyStateHandler: KeyStateHandler, activeCondition: Flag = AlwaysTrue)
	         (implicit log: Logger) =
	{
		val generator = inactive(KeyDownHandler.conditional(activeCondition)())
		generator.start(actorHandler, keyStateHandler)
		generator
	}
	
	/**
	  * Creates a new **inactive** event generator.
	  * @param log Implicit logging implementation
	  * @return A new inactive event generator
	  */
	def inactive()(implicit log: Logger): KeyDownEventGenerator = inactive(KeyDownHandler())
	/**
	  * Creates a new **inactive** event generator.
	  * @param handler Key down -handler to which the generated events will be delivered
	  * @param log Implicit logging implementation
	  * @return A new inactive event generator
	  */
	def inactive(handler: KeyDownHandler)(implicit log: Logger) = new KeyDownEventGenerator(handler)
}

/**
 * An [[utopia.genesis.handling.action.Actor]] that generates [[KeyDownEvent]]s
 * by observing key-presses and -releases over time.
 *
 * Event-generation will start after [[start]](...) has been called
 *
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
class KeyDownEventGenerator(val handler: KeyDownHandler)(implicit log: Logger)
{
	// ATTRIBUTES   --------------------------
	
	private val startedFlag = Settable()
	
	private val keyboardStatePointer = Volatile(KeyboardState.default)
	private val downKeysPointer = Volatile.eventful(Set[(Int, KeyLocation, Instant)]())
	
	private val hasKeysDownFlag: Flag = downKeysPointer.map { _.nonEmpty }
	
	
	// INITIAL CODE --------------------------
	
	// While not listening to events, clears the stored data / state
	handler.handleCondition.addContinuousListener { event =>
		if (!event.newValue)
			downKeysPointer.value = Set()
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Starts event generation
	  * @param actorHandler Actor handler that will deliver the necessary action events
	  * @param keyStateHandler Key-state handler that will deliver key state events
	  */
	def start(actorHandler: ActorHandler, keyStateHandler: KeyStateHandler): Unit = {
		if (startedFlag.set()) {
			keyStateHandler += KeyStateTracker
			actorHandler += Generator
		}
	}
	/**
	  * Starts event generation using the specified handlers
	  * @param handlers A set of handlers which should contain at least an [[ActorHandler]] and a [[KeyStateHandler]].
	  */
	def start(handlers: Handlers): Unit = {
		if (startedFlag.set())
			handlers ++= Pair(KeyStateTracker, Generator)
	}
	
	
	// NESTED   ---------------------------
	
	private object KeyStateTracker extends KeyStateListener
	{
		override def handleCondition: Flag = AlwaysTrue
		override def keyStateEventFilter: KeyStateEventFilter = AcceptAll
		
		override def onKeyState(event: KeyStateEvent): Unit = {
			keyboardStatePointer.value = event.keyboardState
			// Case: Key pressed => Records that the key is now down and remembers the timestamp
			if (event.pressed)
				downKeysPointer.update { _ + ((event.index, event.location, Now.toInstant)) }
			// Case: Key released => Records that the key is no longer down
			else
				downKeysPointer.update { _.filterNot { case (index, location, _) =>
					event.index == index && event.location == location
				} }
		}
	}
	
	private object Generator extends Actor
	{
		// ATTRIBUTES   -------------------
		
		override val handleCondition: Flag = hasKeysDownFlag && handler.handleCondition
		
		
		// IMPLEMENTED  -------------------
		
		override def act(duration: FiniteDuration): Unit = {
			val now = Now.toInstant
			val keyboardState = keyboardStatePointer.value
			downKeysPointer.value.view
				.map { case (index, location, pressTime) =>
					KeyDownEvent(index, location, duration, (now - pressTime) min duration, keyboardState)
				}
				.foreach(handler.whileKeyDown)
		}
	}
}
