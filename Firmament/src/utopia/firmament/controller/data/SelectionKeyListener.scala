package utopia.firmament.controller.data

import utopia.flow.operator.sign.Sign
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.action.Actor
import utopia.genesis.handling.event.keyboard.Key.{DownArrow, LeftArrow, RightArrow, UpArrow}
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D

import scala.concurrent.duration.{Duration, FiniteDuration}

object SelectionKeyListener
{
	/**
	  * Creates a new vertically moving selection key listener. Remember to add it to both an <b>ActorHandler</b> and
	  * a <b>KeyStateHandler</b>
	  * @param listenEnabledCondition A function that is used for testing whether button presses should be recognized
	  *                               (default = always true)
	  * @param initialScrollDelay Delay after key becomes pressed, before selection is moved automatically (provided that
	  *                           the button is still being held down) (default = 0.4 seconds)
	  * @param scrollDelayModifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
	  *                            delay and 1 means same delay as before (default = 0.8)
	  * @param minScrollDelay Minimum delay between each selection move (default = 0.05 seconds)
	  * @param moveSelection A function for moving selection by specified amount
	  * @return A new selection listener
	  */
	def vertical(listenEnabledCondition: => Boolean = true, initialScrollDelay: Duration = 0.4.seconds,
				 scrollDelayModifier: Double = 0.8, minScrollDelay: Duration = 0.05.seconds)
				(moveSelection: Int => Unit) =
		new SelectionKeyListener(DownArrow, UpArrow, listenEnabledCondition, initialScrollDelay, scrollDelayModifier,
			minScrollDelay)(moveSelection)
	
	/**
	  * Creates a new horizontally moving selection key listener. Remember to add it to both an <b>ActorHandler</b> and
	  * a <b>KeyStateHandler</b>
	  * @param listenEnabledCondition A function that is used for testing whether button presses should be recognized
	  *                               (default = always true)
	  * @param initialScrollDelay Delay after key becomes pressed, before selection is moved automatically (provided that
	  *                           the button is still being held down) (default = 0.4 seconds)
	  * @param scrollDelayModifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
	  *                            delay and 1 means same delay as before (default = 0.8)
	  * @param minScrollDelay Minimum delay between each selection move (default = 0.05 seconds)
	  * @param moveSelection A function for moving selection by specified amount
	  * @return A new selection listener
	  */
	def horizontal(listenEnabledCondition: => Boolean = true, initialScrollDelay: Duration = 0.4.seconds,
				   scrollDelayModifier: Double = 0.8, minScrollDelay: Duration = 0.05.seconds)
				  (moveSelection: Int => Unit) =
		new SelectionKeyListener(LeftArrow, RightArrow, listenEnabledCondition, initialScrollDelay, scrollDelayModifier,
			minScrollDelay)(moveSelection)
	
	/**
	  * Creates a new selection key listener. Remember to add it to both an <b>ActorHandler</b> and
	  * a <b>KeyStateHandler</b>
	  * @param axis Axis along which the selection is moved
	  * @param listenEnabledCondition A function that is used for testing whether button presses should be recognized
	  *                               (default = always true)
	  * @param initialScrollDelay Delay after key becomes pressed, before selection is moved automatically (provided that
	  *                           the button is still being held down) (default = 0.4 seconds)
	  * @param scrollDelayModifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
	  *                            delay and 1 means same delay as before (default = 0.8)
	  * @param minScrollDelay Minimum delay between each selection move (default = 0.05 seconds)
	  * @param moveSelection A function for moving selection by specified amount
	  * @return A new selection listener
	  */
	def along(axis: Axis2D, listenEnabledCondition: => Boolean = true, initialScrollDelay: Duration = 0.4.seconds,
			  scrollDelayModifier: Double = 0.8, minScrollDelay: Duration = 0.05.seconds)
			 (moveSelection: Int => Unit) = axis match
	{
		case X => horizontal(listenEnabledCondition, initialScrollDelay, scrollDelayModifier, minScrollDelay)(moveSelection)
		case Y => vertical(listenEnabledCondition, initialScrollDelay, scrollDelayModifier, minScrollDelay)(moveSelection)
	}
}

/**
  * This key listener can be used for triggering selection changes based on key events. Changes selection when user
  * either taps certain keys or when the user continuously holds them down. Remember to add this listener to both
  * KeyStateHandler <b>and</b> ActorHandler
  * @author Mikko Hilpinen
  * @since 14.11.2019, Reflection v1
  * @param nextKey Key for moving selection forward (default = down)
  * @param prevKey Key for moving selection backward (default = up)
  * @param listenEnabledCondition A function that is used for testing whether button presses should be recognized
  *                               (default = always true)
  * @param initialScrollDelay Delay after key becomes pressed, before selection is moved automatically (provided that
  *                           the button is still being held down) (default = 0.4 seconds)
  * @param scrollDelayModifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
  *                            delay and 1 means same delay as before (default = 0.8)
  * @param minScrollDelay Minimum delay between each selection move (default = 0.05 seconds)
  * @param moveSelection A function for moving selection by specified amount
  */
// TODO: Consider managing action-event receiving manually from within this class
class SelectionKeyListener(nextKey: Key = DownArrow, prevKey: Key = UpArrow,
                           listenEnabledCondition: => Boolean = true,
                           initialScrollDelay: Duration = 0.4.seconds, scrollDelayModifier: Double = 0.8,
                           minScrollDelay: Duration = 0.05.seconds)
                          (moveSelection: Int => Unit)
	extends KeyStateListener with Actor
{
	// ATTRIBUTES	-----------------------------
	
	private var buttonDown = false
	private var currentDirection: Sign = Positive
	private var nextDelay = initialScrollDelay
	private var remainingDelay = nextDelay
	
	override val keyStateEventFilter = KeyStateEvent.filter(nextKey, prevKey)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def handleCondition: Flag = AlwaysTrue
	
	override def onKeyState(event: KeyStateEvent) = {
		val direction = if (event.index == nextKey.index) Positive else Negative
		// Case: Key press
		if (event.pressed) {
			// Key presses may be ignored if a special condition is not met
			if (listenEnabledCondition) {
				currentDirection = direction
				remainingDelay = initialScrollDelay
				nextDelay = initialScrollDelay * scrollDelayModifier
				buttonDown = true
				
				// Moves selection 1 step every key press
				moveSelection(direction * 1)
			}
		}
		// Case: held key is being released
		else if (direction == currentDirection)
			buttonDown = false
	}
	
	override def act(duration: FiniteDuration) = {
		// Action events are processed only when a button is being held down
		if (buttonDown) {
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
				moveSelection(currentDirection * move)
		}
	}
}
