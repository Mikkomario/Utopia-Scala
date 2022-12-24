package utopia.flow.view.immutable.eventful

import utopia.flow.async.context.CloseHook
import utopia.flow.async.process.Wait
import utopia.flow.event.listener.ChangeListener
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.{Volatile, VolatileFlag, VolatileOption}
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

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
	@deprecated("Please use source.delayedBy(delay) instead", "v2.0")
	def of[A](source: Changing[A], delay: => Duration)(implicit exc: ExecutionContext) =
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
	
	/**
	  * Creates a new delayed view of another changing item
	  * @param source The viewed item
	  * @param delay A delay to apply to each mirrored change
	  * @param exc Implicit execution context
	  * @tparam A Type of the values in the other item
	  * @return A new delayed view
	  */
	def apply[A](source: Changing[A], delay: FiniteDuration)(implicit exc: ExecutionContext) =
		new DelayedView[A](source, delay)
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
class DelayedView[A](val source: Changing[A], delay: FiniteDuration)(implicit exc: ExecutionContext)
	extends ChangingWrapper[A]
{
	// ATTRIBUTES   --------------------------
	
	private val waitLock = new AnyRef
	
	private val queuedValuePointer = VolatileOption[(A, Instant)]()
	private val valuePointer = Volatile(source.value)
	private val isWaitingFlag = new VolatileFlag()
	
	
	// INITIAL CODE -------------------------
	
	// Whenever source's value changes, delays change activation and updates future value
	source.addListener(ChangeListener.continuous { event =>
		val initialTarget = event.newValue -> (Now + delay)
		val shouldStartWait = queuedValuePointer.pop(old => old.isEmpty -> Some(initialTarget))
		
		// If no waiting is currently active, starts one
		if (shouldStartWait)
			Future {
				// Waits until a pause in changes
				var waitNotBroken = true
				var nextTarget = initialTarget
				while (waitNotBroken && Now < nextTarget._2 && CloseHook.nonShutdown) {
					// If the wait was interrupted, hurries to completion without waiting the prescribed time period
					waitNotBroken = Wait(nextTarget._2, waitLock)
					nextTarget = queuedValuePointer.value.getOrElse(nextTarget)
				}
				// Allows new wait and updates current value
				valuePointer.value = queuedValuePointer.pop() match {
					// Case: Another item was queued after escaping the while-loop (unlikely) =>
					// Immediately applies that value
					case Some((v, _)) => v
					case None => nextTarget._1
				}
			}
	})
	
	
	// IMPLEMENTED -----------------------------
	
	override protected def wrapped = valuePointer
	
	override def isChanging = source.isChanging || isWaitingFlag.isSet
}
