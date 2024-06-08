package utopia.flow.view.immutable.eventful

import utopia.flow.async.context.CloseHook
import utopia.flow.async.process.Wait
import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.time.Now
import utopia.flow.view.mutable.async.{Volatile, VolatileOption}
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

object DelayedView
{
	/**
	  * Creates a new delayed view of another changing item
	  * @param source The viewed item
	  * @param delay A delay to apply to each mirrored change
	  * @param condition Condition that must be met in order for viewing / updating to take place (default = always active)
	  * @param exc Implicit execution context
	  * @tparam A Type of the values in the other item
	  * @return A new delayed view
	  */
	def apply[A](source: Changing[A], delay: FiniteDuration, condition: Changing[Boolean] = AlwaysTrue)
	            (implicit exc: ExecutionContext) =
		new DelayedView[A](source, delay, condition)
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
  * @param condition Condition that must be met in order for viewing / updating to take place (default = always active)
  * @param exc Implicit execution context
  * @tparam A Type of original pointer value
  */
class DelayedView[A](val source: Changing[A], delay: FiniteDuration, condition: Changing[Boolean] = AlwaysTrue)
                    (implicit exc: ExecutionContext)
	extends ChangingWrapper[A]
{
	// ATTRIBUTES   --------------------------
	
	private val waitLock = new AnyRef
	
	private val queuedValuePointer = VolatileOption[(A, Instant)]()
	private val valuePointer = Volatile(source.value)
	
	private var stopListeners: Seq[ChangingStoppedListener] = Empty
	
	
	// INITIAL CODE -------------------------
	
	// Whenever source's value changes, delays change activation and updates future value
	source.addListenerWhile(condition)(ChangeListener.continuous { event =>
		val initialTarget = event.newValue -> (Now + delay)
		val shouldStartWait = queuedValuePointer.mutate { old => old.isEmpty -> Some(initialTarget) }
		
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
	
	// Once the source stops changing, removes listeners and informs stop listeners (delayed)
	source.onceChangingStops {
		queuedValuePointer.onNextChangeWhere { _.newValue.isEmpty } { e =>
			valuePointer.once { v => e.oldValue.forall { _._1 == v } } { _ =>
				valuePointer.clearListeners()
				stopListeners.foreach { _.onChangingStopped() }
				stopListeners = Empty
			}
		}
	}
	
	
	// IMPLEMENTED -----------------------------
	
	override protected def wrapped = valuePointer
	
	override def destiny = source.destiny.fluxIf(queuedValuePointer.nonEmpty)
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener) =
		stopListeners :+= listener
}
