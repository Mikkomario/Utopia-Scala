package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.AcceptAll
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.action.{Actor, ActorHandler}
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object KeyDownEventGenerator
{
	/**
	 * @param actorHandler An actor handler that will deliver action events to this generator
	 * @param keyStateHandler A key-state event handler that will inform this generator of keyboard state changes
	 * @param activeCondition A condition that must be met in order for key-down events to be generated or fired
	 * @return A new functioning key-down event generator
	 */
	def apply(actorHandler: ActorHandler, keyStateHandler: KeyStateHandler, activeCondition: FlagLike = AlwaysTrue) =
	{
		val generator = new KeyDownEventGenerator(KeyDownHandler.conditional(activeCondition)())
		actorHandler += generator
		keyStateHandler += generator
		generator
	}
}

/**
 * An [[utopia.genesis.handling.action.Actor]] that generates [[KeyDownEvent]]s
 * by observing key-presses and -releases over time.
 *
 * Please note that this generator needs to be added to a functioning ActorHandler and KeyStateHandler in order
 * to work correctly.
 *
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
class KeyDownEventGenerator(val handler: KeyDownHandler = KeyDownHandler()) extends Actor with KeyStateListener
{
	// ATTRIBUTES   --------------------------
	
	private val keyboardStatePointer = Volatile(KeyboardState.default)
	private val downKeysPointer = Volatile(Set[(Int, KeyLocation, Instant)]())
	
	private val hasKeysDownFlag: FlagLike = downKeysPointer.map { _.nonEmpty }
	override val handleCondition: FlagLike = hasKeysDownFlag && handler.handleCondition
	
	
	// INITIAL CODE --------------------------
	
	// While not listening to events, clears the stored data / state
	handler.handleCondition.addContinuousListener { event =>
		if (!event.newValue)
			downKeysPointer.value = Set()
	}
	
	
	// IMPLEMENTED  --------------------------
	
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
	
	// Delivers an event for each key that's being held down
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
