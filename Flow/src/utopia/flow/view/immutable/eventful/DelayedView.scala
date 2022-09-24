package utopia.flow.view.immutable.eventful

import utopia.flow.async.context.CloseHook
import utopia.flow.event.listener.{ChangeDependency, ChangeListener}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.template.eventful.{Changing, ChangingLike}

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object DelayedView
{
	/**
	  * Creates a delayed view into the specified item's value. This delayed view will only fire change events after
	  * there's a long enough of period without changes in the original item
	  * @param source A changing item
	  * @param delay Delay to apply to item value changes
	  * @param exc Implicit execution context
	  * @tparam A Type of item values
	  * @return A new delayed view into the item (where all change events are delayed at least by <i>delay</i>)
	  */
	def of[A](source: ChangingLike[A], delay: => Duration)(implicit exc: ExecutionContext) =
	{
		// Won't wrap non-changing items
		if (source.isChanging)
		{
			val cachedDelay = delay
			// If there is no delay, there is no need to wrap the source item
			if (cachedDelay > Duration.Zero)
				cachedDelay.finite match
				{
					case Some(finiteDelay) => new DelayedView(source, finiteDelay)
					case None =>
						// On the other hand, if there is infinite delay, can simply simulate the
						// end result with a fixed value
						Fixed(source.value)
				}
			else
				source
		}
		else
			source
	}
}

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
class DelayedView[A](val source: ChangingLike[A], delay: FiniteDuration)(implicit exc: ExecutionContext)
	extends Changing[A]
{
	// ATTRIBUTES   --------------------------
	
	private val waitLock = new AnyRef
	
	private var changeReactionThreshold = Instant.EPOCH
	private var latestReceivedValue = source.value
	
	private val valuePointer = Volatile(latestReceivedValue)
	private val isWaitingFlag = new VolatileFlag()
	
	
	// INITIAL CODE -------------------------
	
	// Whenever source's value changes, delays change activation and updates future value
	source.addListener(ChangeListener.continuous { event =>
		changeReactionThreshold = Now + delay
		latestReceivedValue = event.newValue
		// If no waiting is currently active, starts one
		if (isWaitingFlag.set())
			Future {
				// Waits until a pause in changes
				var waitNotBroken = true
				while (waitNotBroken && Now < changeReactionThreshold && CloseHook.nonShutdown) {
					// If the wait was interrupted, hurries to completion without waiting the prescribed time period
					waitNotBroken = Wait(changeReactionThreshold, waitLock)
				}
				// Allows new wait and updates current value
				isWaitingFlag.reset()
				valuePointer.value = latestReceivedValue
			}
	})
	
	
	// COMPUTED -----------------------------
	
	// Delegates change handling to the value pointer
	override def value = valuePointer.value
	override def listeners = valuePointer.listeners
	override def listeners_=(newListeners: Vector[ChangeListener[A]]) = valuePointer.listeners = newListeners
	override def dependencies = valuePointer.dependencies
	override def dependencies_=(newDependencies: Vector[ChangeDependency[A]]) =
		valuePointer.dependencies = newDependencies
	override def isChanging = source.isChanging || isWaitingFlag.isSet
}
