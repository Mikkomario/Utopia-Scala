package utopia.flow.async

import java.time.Instant

import utopia.flow.event.{ChangeListener, Changing}
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.WaitUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

/**
  * This view to a changing item waits slightly before updating its value / propagating change events. Only when the
  * source item has stopped changing for a while is the value updated and change event fired. This is useful when
  * you expect there to be multiple changes fired at a time and you want the listeners to only react to react when
  * all of the changes have been observed
  * @author Mikko Hilpinen
  * @since 23.9.2020, v1.9
  * @param source viewed pointer
  * @param delay Required pause between changes before a change event is fired
  * @param exc Implicit execution context
  * @tparam A Type of original pointer value
  */
class DelayedView[A](val source: Changing[A], delay: Duration)(implicit exc: ExecutionContext) extends Changing[A]
{
	// ATTRIBUTES   --------------------------
	
	private val waitLock = new AnyRef
	
	private var changeReactionThreshold = Instant.EPOCH
	private var latestReceivedValue = source.value
	
	private val valuePointer = Volatile(latestReceivedValue)
	private val isWaitingFlag = new VolatileFlag()
	
	
	// INITIAL CODE -------------------------
	
	// Whenever source's value changes, delays change activation and updates future value
	source.addListener { event =>
		changeReactionThreshold = Instant.now() + delay
		latestReceivedValue = event.newValue
		// If no waiting is currently active, starts one
		isWaitingFlag.runAndSet {
			Future
			{
				// Waits until a pause in changes
				while (Instant.now() < changeReactionThreshold)
					WaitUtils.waitUntil(changeReactionThreshold, waitLock)
				// Allows new wait and updates current value
				isWaitingFlag.reset()
				valuePointer.set(latestReceivedValue)
			}
		}
	}
	
	
	// COMPUTED -----------------------------
	
	// Delegates change handling to the value pointer
	override def value = valuePointer.value
	override def listeners = valuePointer.listeners
	override def listeners_=(newListeners: Vector[ChangeListener[A]]) = valuePointer.listeners = newListeners
}
