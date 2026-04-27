package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.WaitTarget.Until
import utopia.flow.async.process.{LoopingProcess, WaitTarget, WaitUtils}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.event.listener.ChangeListener
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object Scheduler
{
	// COMPUTED ----------------------------
	
	/**
	 * @param exc Implicit execution context to use
	 * @param log Implicit logging implementation used
	 * @return A new scheduler
	 */
	def newInstance(implicit exc: ExecutionContext, log: Logger): Scheduler = new _Scheduler()
	
	
	// NESTED   ----------------------------
	
	private class _Scheduler(implicit exc: ExecutionContext, log: Logger) extends LoopingProcess() with Scheduler
	{
		// ATTRIBUTES   --------------------
		
		override protected val isRestartable: Boolean = true
		
		/**
		 * Contains the next events to fire, including targeted timestamps.
		 * Ordered chronologically.
		 */
		private val queueP = Volatile.eventful.emptySeq[(Instant, Boolean => Unit)]
		/**
		 * Contains events that have variable fire times.
		 */
		private val variableQueueP = Volatile.eventful.emptySeq[(Changing[Option[Instant]], Boolean => Unit)]
		
		/**
		 * Contains the next event timestamp, based on the queued static events
		 */
		private val nextQueueTimeP = queueP.strongMap { _.headOption.map { _._1 } }
		/**
		 * Contains the next event timestamp, based on the queued variable events.
		 * Must be updated manually.
		 */
		private val nextVariableQueueTimeP = CopyOnDemand { variableQueueP.value.flatMap { _._1.value }.minOption }
		/**
		 * Contains the next event timestamp.
		 */
		private val nextEventTimeP =
			nextQueueTimeP.strongMergeWith(nextVariableQueueTimeP) { Pair(_, _).flatten.minOption }
		
		/**
		 * A listener that must be attached to all event timestamp pointers while they are being tracked.
		 */
		private val updateVariableQueueTimeListener = ChangeListener.onAnyChange { nextVariableQueueTimeP.update() }
		
		
		// INITIAL CODE --------------------
		
		// Whenever the next event time changes, may reschedule this loop
		nextEventTimeP.addContinuousListener { e =>
			e.oldValue match {
				case Some(previously) =>
					// Case: New wait target is sooner than the previous
					//       => Notifies the wait lock in order to reset the next wait time
					if (e.newValue.exists { _ < previously })
						WaitUtils.notify(waitLock)
					
				// Case: First wait target introduced => Starts this loop (assumes that it was completed previously)
				case None => runAsync()
			}
		}
		
		// Resolves all queued events on JVM shutdown
		CloseHook.registerAction { (queueP.popAll() ++ variableQueueP.popAll()).foreach { _._2(false) } }
		
		
		// IMPLEMENTED  --------------------
		
		override def schedule[A](at: Instant, executeOnJvmShutdown: Boolean)(event: => Future[A]): Future[A] = {
			// Case: Future target time => Schedules / queues the event
			if (at.isFuture) {
				val promise = Promise[A]()
				queueP.update {
					_.insertedBeforeFirstWhere(at -> prepareEvent(promise, event, executeOnJvmShutdown)) { _._1 > at }
				}
				promise.future
			}
			// Case: Past or current target => Executes immediately
			else
				Future.delegate(event)
		}
		override def scheduleVariable[A](targetTimePointer: Changing[Option[Instant]],
		                                 executeOnJvmShutdown: Boolean)
		                                (event: => Future[A]): Future[A] =
		{
			// Case: Fixed target time => Uses a simpler schedule implementation
			if (targetTimePointer.isFixed)
				targetTimePointer.value match {
					case Some(targetTime) => schedule(targetTime)(event)
					// Case: Never scheduled to run => Won't run
					case None => Future.never
				}
			// Case: The current target time is in the future => Schedules the execution
			else if (targetTimePointer.value.forall { _.isFuture }) {
				val promise = Promise[A]()
				variableQueueP :+= (targetTimePointer -> prepareEvent(promise, event, executeOnJvmShutdown))
				
				// Whenever the target time changes, updates this loop, also
				targetTimePointer.addListener(updateVariableQueueTimeListener)
				nextVariableQueueTimeP.update()
				
				// If this task ever becomes canceled, removes it from the queue and marks it as interrupted
				targetTimePointer.onceFixedAt(None) {
					variableQueueP.mutate { _.findAndPop { _._1 != targetTimePointer } }.foreach { _._2(false) }
				}
				
				promise.future
			}
			// Case: Current or past target time => Executes immediately
			else
				Future.delegate(event)
		}
		
		override protected def iteration(): Option[WaitTarget] = {
			// Collects all events that are scheduled to run during this iteration
			val now = Now.toInstant
			val queuedEventsToFire = queueP.mutate { _.splitAtFirstWhere[Seq[(Instant, Boolean => Unit)]] { _._1 > now } }
			val variableEventsToFire = variableQueueP.mutate { _.divideBy { _._1.value.forall { _ > now } }.toTuple }
			
			// Won't listen to target time pointers after the events are executed
			variableEventsToFire.foreach { _._1.removeListener(updateVariableQueueTimeListener) }
			
			// Executes the scheduled events
			(queuedEventsToFire.iterator ++ variableEventsToFire).map { _._2 }
				.foreach { event => exc.execute { () => event(true) } }
			
			// Case: Variable target times were affected => Calculates a new next timestamp
			if (variableEventsToFire.nonEmpty)
				nextVariableQueueTimeP.update()
			
			// Schedules a wait until the next event
			nextEventTimeP.value.map { Until(_) }
		}
		
		
		// OTHER    -----------------------
		
		private def prepareEvent[A](promise: Promise[A], event: => Future[A], executeOnJvmShutdown: Boolean): Boolean => Unit =
		{
			normalRun =>
				if (normalRun || executeOnJvmShutdown)
					Try(event) match {
						case Success(future) =>
							if (normalRun)
								promise.completeWith(future)
							// Case: JVM shutdown with execution => Blocks until resolved
							else
								promise.complete(future.waitFor())
							
						case Failure(error) =>
							log(error, "Unexpected failure during a scheduled event")
							promise.failure(error)
					}
				// Case: JVM shutdown & cancel => Fails with an InterruptedException
				else
					promise.failure(new InterruptedException("JVM shutdown before the scheduled event"))
		}
	}
}

/**
 * An interface for scheduling events to occur at specific timestamps
 * @author Mikko Hilpinen
 * @since 24.04.2026, v2.9
 */
trait Scheduler
{
	/**
	 * Schedules an event to be executed at a specific timestamp
	 * @param at Time when 'event' should be executed
	 * @param executeOnJvmShutdown Whether the 'event' should be executed in case the JVM shuts down before 'at'.
	 *                             Default = false = No execution will be performed on JVM shutdown,
	 *                             and the resulting future will be failed with an InterruptedException.
	 * @param event Event to execute. Yields a future.
	 * @tparam A Return value of the generated future
	 * @return A future that resolves once 'event' has been executed and fully resolved.
	 */
	def schedule[A](at: Instant, executeOnJvmShutdown: Boolean = false)(event: => Future[A]): Future[A]
	/**
	 * Schedules an event to be executed at the first current or past timestamp specified by a pointer
	 * @param targetTimePointer A pointer that contains the time when 'event' should be executed.
	 *                          Contains None while the event is not to be executed.
	 * @param executeOnJvmShutdown Whether the 'event' should be executed in case the JVM shuts down before 'at'.
	 *                             Default = false = No execution will be performed on JVM shutdown,
	 *                             and the resulting future will be failed with an InterruptedException.
	 * @param event Event to execute. Yields a future.
	 * @tparam A Return value of the generated future
	 * @return A future that resolves once 'event' has been executed and fully resolved.
	 *         NB: Depending on the target time pointer, this future might never resolve.
	 */
	def scheduleVariable[A](targetTimePointer: Changing[Option[Instant]], executeOnJvmShutdown: Boolean = false)
	                       (event: => Future[A]): Future[A]
}
