package utopia.reflection.controller.data

import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}
import utopia.flow.time.TimeExtensions._
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.{Actor, ActorHandlerType, KeyStateListener}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable

import java.awt.event.KeyEvent
import scala.concurrent.duration.{Duration, FiniteDuration}

object SelectionKeyListener2
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
				(moveSelection: Int => Unit) = new SelectionKeyListener2(KeyEvent.VK_DOWN, KeyEvent.VK_UP,
		listenEnabledCondition, initialScrollDelay, scrollDelayModifier, minScrollDelay)(moveSelection)
	
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
				  (moveSelection: Int => Unit) = new SelectionKeyListener2(KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
		listenEnabledCondition, initialScrollDelay, scrollDelayModifier, minScrollDelay)(moveSelection)
	
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
  * @since 14.11.2019, v1
  * @param nextKeyCode Key code for moving selection forward (default = down)
  * @param prevKeyCode Key code for moving selection backward (default = up)
  * @param listenEnabledCondition A function that is used for testing whether button presses should be recognized
  *                               (default = always true)
  * @param initialScrollDelay Delay after key becomes pressed, before selection is moved automatically (provided that
  *                           the button is still being held down) (default = 0.4 seconds)
  * @param scrollDelayModifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
  *                            delay and 1 means same delay as before (default = 0.8)
  * @param minScrollDelay Minimum delay between each selection move (default = 0.05 seconds)
  * @param moveSelection A function for moving selection by specified amount
  */
class SelectionKeyListener2(nextKeyCode: Int = KeyEvent.VK_DOWN, prevKeyCode: Int = KeyEvent.VK_UP,
							listenEnabledCondition: => Boolean = true,
							initialScrollDelay: Duration = 0.4.seconds, scrollDelayModifier: Double = 0.8,
							minScrollDelay: Duration = 0.05.seconds)
						   (moveSelection: Int => Unit)
	extends KeyStateListener with Actor with Handleable
{
	// ATTRIBUTES	-----------------------------
	
	private var buttonDown = false
	private var currentDirection: Sign = Positive
	private var nextDelay = initialScrollDelay
	private var remainingDelay = nextDelay
	
	
	// IMPLEMENTED	-----------------------------
	
	override val keyStateEventFilter = KeyStateEvent.keysFilter(nextKeyCode, prevKeyCode)
	
	override def allowsHandlingFrom(handlerType: HandlerType) = handlerType match
	{
		// Action events are received only when a button is being held down
		case ActorHandlerType => buttonDown
		case _ => true
	}
	
	override def onKeyState(event: KeyStateEvent) =
	{
		val direction = if (event.index == nextKeyCode) Positive else Negative
		// Case: Key press
		if (event.isDown)
		{
			// Key presses may be ignored if a special condition is not met
			if (listenEnabledCondition)
			{
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
	
	override def act(duration: FiniteDuration) =
	{
		// Moves towards the next "tick"
		remainingDelay -= duration
		
		// Checks whether there should be any, or multiple, "ticks" during this action event
		var move = 0
		while (remainingDelay <= Duration.Zero)
		{
			move += 1
			remainingDelay += nextDelay max minScrollDelay
			nextDelay *= scrollDelayModifier
		}
		if (move != 0)
			moveSelection(currentDirection * move)
	}
}
