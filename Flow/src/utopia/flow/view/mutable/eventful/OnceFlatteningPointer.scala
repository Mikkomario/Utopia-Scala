package utopia.flow.view.mutable.eventful

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.event.listener.{ChangeListener, ChangingStoppedListener}
import utopia.flow.event.model.Destiny.MaySeal
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.operator.enumeration.End
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.View
import utopia.flow.view.template.eventful.{Changing, ChangingWrapper}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object OnceFlatteningPointer
{
	/**
	  * Creates a new pointer
	  * @param placeholderValue A placeholder value given until the pointer-to-wrap has been specified
	  * @tparam A Type of values returned by this pointer
	  * @return A new pointer that may be completed later
	  */
	def apply[A](placeholderValue: A) = new OnceFlatteningPointer[A](placeholderValue)
	
	/**
	  * Creates a new pointer that will wrap another pointer from a future, once that future resolves.
	  * @param future A future that will resolve into the pointer to wrap.
	  *               This future is not expected to fail;
	  *                     If this future contains an immediate failure, it is thrown.
	  *                     Other failures are reported to the specified logging implementation.
	  * @param placeholderValue A value to return until the future resolves (call-by-name).
	  *                         Not called in situations where the future has already resolved.
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation used for handling situations where:
	  *                 1. A listener assigned to this pointer throws an exception
	  *                    while it is being transferred to the newly acquired pointer
	  *                 1. The specified future fails after this method call
	  * @tparam A Type of values returned by this pointer
	  * @return A new pointer that will match the future pointer in function, after that future has resolved.
	  */
	def wrap[A](future: Future[Changing[A]], placeholderValue: => A)(implicit exc: ExecutionContext, log: Logger) =
		future.currentResult match {
			case Some(immediate) => immediate.get
			case None =>
				val waiter = apply(placeholderValue)
				future.onComplete {
					case Success(pointer) => waiter.complete(pointer)
					case Failure(error) => log(error, "Pointer future failed")
				}
				waiter
		}
}

/**
  * A pointer that allows only one modification, where it is set to match another pointer.
  * Useful in situations where a pointer is required but is not initially available.
  * @author Mikko Hilpinen
  * @since 25.7.2023, v2.2
  */
class OnceFlatteningPointer[A](placeholderValue: A) extends Changing[A]
{
	// ATTRIBUTES   ------------------------
	
	// The listeners are stored until the pointer is appropriated
	private var queuedListeners = Pair.twice[Seq[ChangeListener[A]]](Empty)
	private var queuedStopListeners: Seq[ChangingStoppedListener] = Empty
	
	private var pointer: Option[Changing[A]] = None
	
	override lazy val readOnly: Changing[A] = if (pointer.isDefined) this else ChangingWrapper(this)
	
	
	// IMPLEMENTED  ------------------------
	
	override implicit def listenerLogger: Logger = SysErrLogger
	
	override def value: A = pointer match {
		case Some(p) => p.value
		case None => placeholderValue
	}
	override def destiny: Destiny = pointer match {
		case Some(p) => p.destiny
		case None => MaySeal
	}
	
	override def hasListeners: Boolean = pointer match {
		case Some(p) => p.hasListeners
		case None => queuedListeners.exists { _.nonEmpty }
	}
	override def numberOfListeners: Int = pointer match {
		case Some(p) => p.numberOfListeners
		case None => queuedListeners.map { _.size }.sum
	}
	
	override protected def _addListenerOfPriority(priority: End, lazyListener: View[ChangeListener[A]]): Unit =
		pointer match {
			// Case: Pointer already defined => Assigns listeners directly to it
			case Some(p) => p.addListenerOfPriority(priority)(lazyListener.value)
			// Case: No pointer yet available => Queues the listeners
			case None =>
				queuedListeners = queuedListeners.mapSide(priority) { q =>
					if (q.contains(lazyListener.value)) q else q :+ lazyListener.value
				}
		}
	
	override def removeListener(changeListener: Any): Unit = pointer match {
		case Some(p) => p.removeListener(changeListener)
		case None => queuedListeners = queuedListeners.map { _.filterNot { _ == changeListener } }
	}
	
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = pointer match {
		case Some(p) => p.addChangingStoppedListener(listener)
		case None => queuedStopListeners :+= listener
	}
	
	
	// OTHER    -------------------------
	
	/**
	  * Completes this pointer by defining the pointer which to wrap afterwards.
	  * This function may only be called once. Every other call will throw.
	  * @param pointer A pointer to wrap from now-on.
	  * @throws IllegalStateException If a (different) pointer had already been defined.
	  */
	@throws[IllegalStateException]("If a pointer has already been defined")
	def complete(pointer: Changing[A]) = {
		if (this.pointer.isEmpty) {
			// Assigns the pointer
			this.pointer = Some(pointer)
			// Moves the queued listeners over
			val newValue = pointer.value
			if (queuedListeners.exists { _.nonEmpty }) {
				// Case: "Simulated" value changes => Fires a simulated change event for all the listeners
				if (newValue != placeholderValue) {
					val event = ChangeEvent(placeholderValue, newValue)
					val afterEffects = End.values.flatMap { prio =>
						// Listener response to this event dictates
						// whether it shall be transferred over or simply discarded
						// The after-effects are bundled for later
						val (listenersToTransfer, afterEffects) = queuedListeners(prio).splitFlatMap { listener =>
							// Catches possible failures thrown by listeners
							Try { listener.onChangeEvent(event) } match {
								case Success(response) =>
									(if (response.shouldContinueListening) Some(listener) else None) ->
										response.afterEffects
								// Case: Listener threw => Logs the error and continues as if the listener
								//                         had accepted the event and wishes to continue
								case Failure(error) =>
									pointer.listenerLogger(error,
										s"A queued change listener threw an exception when reacting to a generated change event: $event")
									Some(listener) -> Empty
							}
						}
						// Transfers the remaining listeners
						listenersToTransfer.foreach { pointer.addListenerOfPriority(prio)(_) }
						afterEffects
					}
					// Triggers the after-effects once all listeners have been called and transferred over
					afterEffects.foreach { _() }
				}
				// Case: No simulated value change required, simply transfers the listeners
				else
					queuedListeners.foreachSide { (listeners, prio) =>
						listeners.foreach { pointer.addListenerOfPriority(prio)(_) }
					}
				
				// Discards the listeners from this instance afterwards
				queuedListeners = Pair.twice(Empty)
			}
			
			// Moves the stop listeners over as well
			queuedStopListeners.foreach { pointer.addChangingStoppedListenerAndSimulateEvent(_) }
			queuedStopListeners = Empty
		}
		else if (!this.pointer.contains(pointer))
			throw new IllegalStateException("Pointer had already been assigned")
	}
	
	/**
	  * Completes this pointer by defining the pointer which to wrap afterwards.
	  * If this pointer has already been completed, this method does nothing.
	  * @param pointer A pointer to wrap (call-by-name).
	  * @return Whether this pointer was completed. False if this was already completed.
	  */
	def tryComplete(pointer: => Changing[A]) = {
		if (this.pointer.isEmpty) {
			complete(pointer)
			true
		}
		else
			false
	}
}
