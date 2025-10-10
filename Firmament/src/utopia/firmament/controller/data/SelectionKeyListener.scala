package utopia.firmament.controller.data

import utopia.flow.operator.filter.AcceptAll
import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Mutate
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.{AlwaysTrue, Fixed}
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.handling.action.Actor
import utopia.genesis.handling.event.keyboard.Key.{DownArrow, LeftArrow, RightArrow, UpArrow}
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions

object SelectionKeyListener
{
	// COMPUTED -----------------------------
	
	/**
	 * @param log Implicit logging implementation used in pointer management
	 * @return A listener factory
	 */
	def factory(implicit log: Logger) = SelectionKeyListenerFactory()
	
	
	// IMPLICIT -----------------------------
	
	// Implicitly converts this object into a key-listener factory
	implicit def objectToFactory(o: SelectionKeyListener.type)(implicit log: Logger): SelectionKeyListenerFactory =
		o.factory
	
	
	// OTHER    -----------------------------
	
	@deprecated("Deprecated for removal. Please use .factory instead", "v1.6")
	def vertical(implicit log: Logger) = factory
	@deprecated("Deprecated for removal. Please use .factory.listeningToKeysAlong(Axis2D) instead", "v1.6")
	def along(axis: Axis2D)(implicit log: Logger) = factory.listeningToKeysAlong(axis)
	
	
	// NESTED   -----------------------------
	
	case class SelectionKeyListenerFactory(keysPointer: Changing[Map[Int, Sign]] = Fixed(Map(UpArrow.index -> Negative, DownArrow.index -> Positive)),
	                                       enabledFlag: Flag = AlwaysTrue, initialScrollDelay: Duration = 0.4.seconds,
	                                       scrollDelayModifier: Double = 0.8, minScrollDelay: Duration = 0.05.seconds,
	                                       additionalCondition: View[Boolean] = AlwaysTrue)
	                                      (implicit log: Logger)
	{
		// COMPUTED ------------------------
		
		/**
		 * @return A factory where the listener reacts to horizontal arrow keys
		 */
		def horizontal = listeningTo(LeftArrow, RightArrow)
		
		
		// OTHER    ------------------------
		
		/**
		 * @param enabledFlag A flag that contains true while key-listening should be enabled
		 * @return Copy of this factory using the specified enabled pointer
		 */
		def withEnabledFlag(enabledFlag: Flag) = copy(enabledFlag = enabledFlag)
		/**
		 * @param condition a condition (function) that must be met for selection change to occur
		 * @return Copy of this factory applying the specified extra condition
		 * @see [[withEnabledFlag]] - Prefer this function if at all possible
		 */
		def withAdditionalCondition(condition: => Boolean): SelectionKeyListenerFactory =
			withAdditionalCondition(View(condition))
		/**
		 * @param condition A view into a condition that must be met for selection change to occur
		 * @return Copy of this factory applying the specified extra condition
		 * @see [[withEnabledFlag]] - Prefer this function if at all possible
		 */
		def withAdditionalCondition(condition: View[Boolean]) = condition match {
			case p: Changing[Boolean] => copy(enabledFlag = enabledFlag && p)
			case c => copy(additionalCondition = c)
		}
		
		/**
		 * @param delay Delay after key becomes pressed, before selection is moved automatically
		 *              (provided that the button is still being held down)
		 * @return Copy of this factory with the specified delay
		 */
		def withInitialScrollDelay(delay: Duration) = copy(initialScrollDelay = delay)
		def mapInitialScrollDelay(f: Mutate[Duration]) =
			withInitialScrollDelay(f(initialScrollDelay))
		
		/**
		 * @param modifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
		 *                 delay and 1 means same delay as before.
		 * @return Copy of this factory using the specified delay modifier
		 */
		def withScrollDelayModifier(modifier: Double) =
			copy(scrollDelayModifier = modifier)
		def mapScrollDelayModifier(f: Mutate[Double]) =
			withScrollDelayModifier(f(scrollDelayModifier))
		
		/**
		 * @param delay Minimum delay between each selection move
		 * @return Copy of this factory using the specified minimum delay
		 */
		def withMinScrollDelay(delay: Duration) = copy(minScrollDelay = delay)
		def mapMinScrollDelay(f: Mutate[Duration]) = withMinScrollDelay(f(minScrollDelay))
		
		def mapScrollDelays(f: Mutate[Duration]) =
			copy(initialScrollDelay = f(initialScrollDelay), minScrollDelay = f(minScrollDelay))
		
		/**
		 * @param back Key for selecting the previous item
		 * @param forward Key for selecting the next item
		 * @return A copy of this factory where the listener reacts to the specified keys
		 */
		def listeningTo(back: Key, forward: Key): SelectionKeyListenerFactory =
			listeningTo(Map(back -> Negative, forward -> Positive))
		/**
		 * @param keys A map where keys are keyboard keys and values are directions to which they move the selection.
		 *             Should not be empty.
		 * @return A copy of this factory where the listener reacts to the specified keys.
		 */
		def listeningTo(keys: Map[Key, Sign]) =
			copy(keysPointer = Fixed(keys.map { case (key, direction) => key.index -> direction }))
		/**
		 * @param keysPointer A pointer containing map where keys are keyboard keys and values are directions
		 *                    to which they move the selection.
		 * @return A copy of this factory where the listener reacts to the keys indicated by the specified pointer.
		 */
		def listeningTo(keysPointer: Changing[Map[Key, Sign]]) =
			copy(keysPointer = keysPointer.map { _.map { case (key, direction) => key.index -> direction } })
		
		/**
		 * @param axis An axis
		 * @return A copy of this factory where the listener reacts to arrow keys along that axis
		 */
		def listeningToKeysAlong(axis: Axis2D) = axis match {
			case X => horizontal
			case Y => listeningTo(UpArrow, DownArrow)
		}
		/**
		 * @param axisPointer A pointer that contains the tracked axis
		 * @return A copy of this factory where the listener reacts to arrow keys along the axis indicated by the
		 *         specified pointer
		 */
		def listeningToKeysAlong(axisPointer: Changing[Axis2D]) =
			copy(keysPointer = axisPointer.map {
				case X => Map(LeftArrow.index -> Negative, RightArrow.index -> Positive)
				case Y => Map(UpArrow.index -> Negative, DownArrow.index -> Positive)
			})
		
		/**
		 * @param moveSelection A function called when selection should be adjusted.
		 *                      Receives the number of "steps" to move the selection (usually 1 or -1).
		 * @return A new listener for moving the selection.
		 *
		 *         Note: The listener must be added to a working [[utopia.genesis.handling.action.ActorHandler]] and
		 *         [[utopia.genesis.handling.event.keyboard.KeyStateHandler]]
		 */
		def apply(moveSelection: Int => Unit) =
			new SelectionKeyListener(keysPointer, enabledFlag, initialScrollDelay, scrollDelayModifier, minScrollDelay,
				additionalCondition)(moveSelection)
	}
}

/**
  * This key listener can be used for triggering selection changes based on key events. Changes selection when user
  * either taps certain keys or when the user continuously holds them down. Remember to add this listener to both
  * KeyStateHandler <b>and</b> ActorHandler
  * @author Mikko Hilpinen
  * @since 14.11.2019, Reflection v1
 * @param keysPointer A pointer that contains a map where keys are keyboard key indices and values are the matching
 *                    selection directions. This listener will listen on those keys.
 * @param enabledFlag A flag that contains true while listening is allowed.
  * @param initialScrollDelay Delay after key becomes pressed, before selection is moved automatically (provided that
  *                           the button is still being held down) (default = 0.4 seconds)
  * @param scrollDelayModifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
  *                            delay and 1 means same delay as before (default = 0.8)
  * @param minScrollDelay Minimum delay between each selection move (default = 0.05 seconds)
  * @param additionalListenCondition A view into an additional condition that must be met for selection listening to occur.
 *                                  Useful in situations where the enabled state can't be represented using a pointer.
 * @param moveSelection A function for moving selection by specified amount
 * @param log Implicit logging implementation. Used in pointer management.
  */
// TODO: Consider managing action-event receiving manually from within this class
// Note: Once/if we phase out Reflection, remove the 'additionalListenCondition' feature.
class SelectionKeyListener(keysPointer: Changing[Map[Int, Sign]], enabledFlag: Flag, initialScrollDelay: Duration,
                           scrollDelayModifier: Double, minScrollDelay: Duration,
                           additionalListenCondition: View[Boolean] = AlwaysTrue)
                          (moveSelection: Int => Unit)(implicit log: Logger)
	extends KeyStateListener with Actor
{
	// ATTRIBUTES	-----------------------------
	
	private val scrollDirectionP = Pointer.eventful.empty[Sign]
	
	private var nextDelay = initialScrollDelay
	private var remainingDelay = nextDelay
	
	override val handleCondition: Flag = enabledFlag && keysPointer.lightMap { _.nonEmpty }
	override val keyStateEventFilter = AcceptAll
	
	
	// INITIAL CODE -----------------------------
	
	// If key-listening is disabled for some reason, ends scrolling automatically
	if (handleCondition.mayChange) {
		val scrollingFlag = scrollDirectionP.lightMap { _.isDefined }
		enabledFlag.addListenerWhile(scrollingFlag) { e =>
			if (!e.newValue)
				scrollDirectionP.clear()
		}
	}
	
	
	// IMPLEMENTED	-----------------------------
	
	override def onKeyState(event: KeyStateEvent) =
		keysPointer.value.get(event.index).foreach { direction =>
			// Case: Key press
			if (event.pressed) {
				// May require an additional condition
				if (additionalListenCondition.value) {
					scrollDirectionP.setOne(direction)
					remainingDelay = initialScrollDelay
					nextDelay = initialScrollDelay * scrollDelayModifier
					
					// Moves selection 1 step every key press
					moveSelection(direction * 1)
				}
			}
			// Case: held key is being released
			else
				scrollDirectionP.update { _.filterNot { _ == direction } }
		}
	
	override def act(duration: FiniteDuration) = {
		// Action events are processed only when a button is being held down
		scrollDirectionP.value.foreach { direction =>
			// Moves towards the next "tick"
			remainingDelay -= duration
			
			// Checks whether there should be any, or multiple, "ticks" during this action event
			var move = 0
			while (remainingDelay <= Duration.Zero) {
				move += 1
				remainingDelay += nextDelay max minScrollDelay
				nextDelay *= scrollDelayModifier
			}
			if (move != 0)
				moveSelection(direction * move)
		}
	}
}
