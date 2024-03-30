package utopia.genesis.handling.event.keyboard
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.keyboard.KeyDownListener.KeyDownEventFilter
import utopia.genesis.handling.event.keyboard.KeyStateListener.KeyStateEventFilter

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object HoldKeyToTypeGenerator
{
	/**
	 * Creates a new generator and attaches it to the required event handlers, starting the event-generation process.
	 * @param keyDownHandler A handler that delivers key-down events to this generator
	 * @param keyStateHandler A handler that delivers key released -events to this generator
	 * @param keyTypedHandler A handler that delivers key typed -events to this generator
	 * @param listener A listener that receives the generated key typed -events
	 * @param initialDelay he duration of time a key must be held down before key typed -events are generated
	 *                     (default = 0.8 seconds)
	 * @param eventInterval After 'initialDelay' has passed and while the key is still held down,
	 *                      how often are key typed -events generated.
	 *                      Default = once every 0.2 seconds.
	 * @param condition A condition that must be met for event-generation to occur.
	 *                  Default = no condition applied.
	 * @param filter A filter applied to incoming key-down events.
	 *               Default = no filtering applied.
	 */
	def start(keyDownHandler: KeyDownHandler, keyStateHandler: KeyStateHandler, keyTypedHandler: KeyTypedHandler,
	          listener: KeyTypedListener, initialDelay: FiniteDuration = 0.8.seconds,
	          eventInterval: FiniteDuration = 0.2.seconds,
	          condition: FlagLike = AlwaysTrue, filter: KeyDownEventFilter = AcceptAll): Unit =
	{
		val generator = new HoldKeyToTypeGenerator(listener, initialDelay, eventInterval, condition, filter)
		keyDownHandler += generator
		keyStateHandler += generator
		keyTypedHandler += generator
	}
}

/**
 * Generates key typed -events when a keyboard key is being held down.
 *
 * In order to function properly, this generator needs to be added to a
 * KeyDownHandler, a KeyStateHandler and a KeyTypedHandler.
 *
 * @constructor Creates a new event generator
 * @param listener The listener informed of generated key typed -events
 * @param initialDelay The duration of time a key must be held down before key typed -events are generated
 *                     (default = 0.8 seconds)
 * @param eventInterval After 'initialDelay' has passed and while the key is still held down,
 *                      how often are key typed -events generated.
 *                      Default = once every 0.2 seconds.
 * @param condition A condition that must be met for event-generation to occur.
 *                  Default = no condition applied.
 * @param filter A filter applied to incoming key-down events.
 *               Default = no filtering applied.
 *
 * @author Mikko Hilpinen
 * @since 29/03/2024, v4.0
 */
class HoldKeyToTypeGenerator(listener: KeyTypedListener, initialDelay: FiniteDuration = 0.8.seconds,
                             eventInterval: FiniteDuration = 0.2.seconds,
                             condition: FlagLike = AlwaysTrue, filter: KeyDownEventFilter = AcceptAll)
	extends KeyDownListener with KeyStateListener with KeyTypedListener
{
	// ATTRIBUTES   -----------------------
	
	override lazy val handleCondition: FlagLike = condition && listener.handleCondition
	override val keyDownEventFilter: KeyDownEventFilter = filter && KeyDownEvent.filter.after(initialDelay)
	override val keyStateEventFilter: KeyStateEventFilter = KeyStateEventFilter.released
	
	// Key is key index
	// Value is typed character + delay until next event is fired
	private val eventDelays = mutable.Map[Int, (Char, FiniteDuration)]()
	
	
	// INITIAL CODE -----------------------
	
	// If stops listening to input, clears all queued events
	handleCondition.addContinuousListener { event =>
		if (!event.newValue)
			eventDelays.clear()
	}
	
	
	// IMPLEMENTED  -----------------------
	
	override def keyTypedEventFilter: Filter[KeyTypedEvent] = listener.keyTypedEventFilter
	
	// Case: Key typed => Prepares for firing events
	override def onKeyTyped(event: KeyTypedEvent): Unit = eventDelays(event.index) = (event.typedChar -> eventInterval)
	// Case: Key released => Won't fire events for it anymore
	override def onKeyState(event: KeyStateEvent): Unit = eventDelays.remove(event.index)
	
	// Case: A key is being held down for an extended period of time => Tracks delay and fires events every so often
	override def whileKeyDown(event: KeyDownEvent): Unit = {
		// Checks whether the held key is one of the recently typed keys
		eventDelays.get(event.index).foreach { case (character, delay) =>
			// Checks whether an event should be fired now or later
			val (delayAfter, shouldFireEvent) = {
				// Case: Fire now
				if (delay <= event.duration)
					(delay + eventInterval - event.duration) -> true
				// Case: Fire later
				else
					(delay - event.duration) -> false
			}
			// Schedules the next event
			eventDelays(event.index) = (character -> delayAfter)
			// Fires event now, if appropriate (and if accepted by the listener)
			if (shouldFireEvent) {
				val typedEvent = KeyTypedEvent(character, event.index, KeyboardEvents.state)
				if (listener.keyTypedEventFilter(typedEvent))
					listener.onKeyTyped(typedEvent)
			}
		}
	}
}
