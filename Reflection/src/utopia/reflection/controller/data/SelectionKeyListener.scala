package utopia.reflection.controller.data

import java.awt.event.KeyEvent

import scala.concurrent.duration.{Duration, FiniteDuration}
import utopia.flow.util.TimeExtensions._
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.{Actor, ActorHandlerType, KeyStateListener}
import utopia.inception.handling.HandlerType
import utopia.inception.handling.immutable.Handleable

/**
  * This key listener can be used for triggering selection changes based on key events. Changes selection when user
  * either taps certain keys or when the user continuously holds them down. Remember to add this listener to both
  * KeyStateHandler <b>and</b> ActorHandler
  * @author Mikko Hilpinen
  * @since 14.11.2019, v1+
  * @param nextKeyCode Key code for moving selection forward (default = down)
  * @param prevKeyCode Key code for moving selection backward (default = up)
  * @param initialScrollDelay Delay after key becomes pressed, before selection is moved automatically (provided that
  *                           the button is still being held down)
  * @param scrollDelayModifier A modifier applied to next scroll step delay after each step ]0, 1] where 0 means no
  *                            delay and 1 means same delay as before
  * @param minScrollDelay Minimum delay between each selection move (default = 0.05 seconds)
  * @param moveSelection A function for moving selection by specified amount
  */
class SelectionKeyListener(val nextKeyCode: Int = KeyEvent.VK_DOWN, val prevKeyCode: Int = KeyEvent.VK_UP,
						   val initialScrollDelay: Duration = 0.4.seconds, val scrollDelayModifier: Double = 0.8,
						   val minScrollDelay: Duration = 0.05.seconds,
						   private val listenEnabledCondition: Option[() => Boolean] = None)
						  (private val moveSelection: Int => Unit) extends KeyStateListener with Actor with Handleable
{
	// ATTRIBUTES	-----------------------------
	
	private var isButtonDown = false
	private var currentDirection = 0
	private var nextDelay = initialScrollDelay
	private var remainingDelay = nextDelay
	
	
	// IMPLEMENTED	-----------------------------
	
	override val keyStateEventFilter = KeyStateEvent.keysFilter(nextKeyCode, prevKeyCode)
	
	override def allowsHandlingFrom(handlerType: HandlerType) = handlerType match
	{
		// Action events are received only when a button is being held down
		case ActorHandlerType => isButtonDown
		case _ => super.allowsHandlingFrom(handlerType)
	}
	
	override def onKeyState(event: KeyStateEvent) =
	{
		val direction = if (event.index == nextKeyCode) 1 else if (event.index == prevKeyCode) -1 else 0
		if (direction != 0)
		{
			// Case: Key press
			if (event.isDown)
			{
				// Key presses may be ignored if a special condition is not met
				if (listenEnabledCondition.forall { _() })
				{
					currentDirection = direction
					remainingDelay = initialScrollDelay
					nextDelay = initialScrollDelay * scrollDelayModifier
					isButtonDown = true
					
					// Moves selection 1 step every key press
					moveSelection(direction)
				}
			}
			// Case: held key is being released
			else if (direction == currentDirection)
				isButtonDown = false
		}
	}
	
	override def act(duration: FiniteDuration) =
	{
		// Moves towards the next "tick"
		remainingDelay -= duration
		
		// Checks whether there should be any, or multiple, "ticks" during this action event
		var move = 0
		while (remainingDelay <= Duration.Zero)
		{
			move += currentDirection
			remainingDelay += nextDelay max minScrollDelay
			nextDelay *= scrollDelayModifier
		}
		if (move != 0)
			moveSelection(move)
	}
}
