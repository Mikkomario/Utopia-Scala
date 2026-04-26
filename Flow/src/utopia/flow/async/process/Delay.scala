package utopia.flow.async.process

import utopia.flow.async.context.Scheduler
import utopia.flow.async.process.ShutdownReaction.{Cancel, DelayShutdown, SkipDelay}
import utopia.flow.collection.immutable.Pair
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Settable
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * An object used for performing delayed operations
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
object Delay
{
	// ATTRIBUTES   ---------------------
	
	private val simpleFactories = Pair(false, true).map { new SimpleDelayFactory(_) }
	private val advancedFactory = AdvancedDelayFactory()
	
	
	// IMPLICIT -------------------------
	
	// Implicitly converts this object into the default factories
	implicit def objectAsSimpleFactory(o: Delay.type): SimpleDelayFactory = o.simpleFactories.first
	implicit def objectAsAdvancedFactory(o: Delay.type): AdvancedDelayFactory = o.advancedFactory
	
	
	// NESTED   -----------------------------
	
	object SimpleDelayFactory
	{
		implicit def toAdvanced(simple: SimpleDelayFactory): AdvancedDelayFactory = simple.advanced
	}
	class SimpleDelayFactory(executesOnJvmShutdown: Boolean = false)
	{
		// ATTRIBUTES   ---------------------
		
		private val advanced = AdvancedDelayFactory(shutdownReaction = if (executesOnJvmShutdown) SkipDelay else Cancel)
		
		
		// COMPUTED -------------------------
		
		/**
		 * @return A copy of this factory that executes the delayed action before allowing JVM to shut down.
		 *         The delay itself is skipped, however.
		 */
		def executingOnShutdown = simpleFactories.second
		
		
		// OTHER    -------------------------
		
		/**
		 * Schedules an asynchronous action
		 * @param duration Duration that must pass before 'f' is triggered
		 * @param f A function that's executed once 'duration' has passed
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of 'f'
		 * @return A future that resolves once 'f' has been called
		 */
		def after[A](duration: Duration)(f: => A)(implicit scheduler: Scheduler) =
			futureAfter(duration) { Future.successful(f) }
		/**
		 * Schedules an asynchronous action
		 * @param target Targeted triggering time of 'f'
		 * @param f A function that's executed once 'target' is reached
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of 'f'
		 * @return A future that resolves once 'f' has been called
		 */
		def until[A](target: Instant)(f: => A)(implicit scheduler: Scheduler) =
			futureFrom(target) { Future.successful(f) }
		
		/**
		 * Schedules an asynchronous action
		 * @param duration Duration that must pass before 'f' is triggered
		 * @param f A function that's executed once 'duration' has passed. Yields a future.
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of 'f'
		 * @return A future that resolves once 'f' has been called and fully resolved
		 */
		def futureAfter[A](duration: Duration)(f: => Future[A])
		                  (implicit scheduler: Scheduler): Future[A] =
		{
			if (duration.isFinite)
				futureFrom(Now + duration)(f)
			else
				Future.never
		}
		/**
		 * Schedules an asynchronous action
		 * @param at Targeted triggering time of 'f'
		 * @param f A function that's executed once 'at' is reached. Yields a future.
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of 'f'
		 * @return A future that resolves once 'f' has been called and fully resolved.
		 */
		def futureFrom[A](at: Instant)(f: => Future[A])(implicit scheduler: Scheduler): Future[A] =
			scheduler.schedule(at, executesOnJvmShutdown)(f)
		
		/**
		 * Schedules an asynchronous action
		 * @param targetPointer A pointer that contains the timestamp when 'f' should be triggered.
		 *                      May also contain None, in case 'f' is delayed indefinitely.
		 * @param f A function that's executed once 'targetPointer' contains a current or past timestamp
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of 'f'
		 * @return A future that resolves once 'f' has been called
		 */
		def variable[A](targetPointer: Changing[Option[Instant]])(f: => A)(implicit scheduler: Scheduler) =
			variableFuture(targetPointer) { Future.successful(f) }
		/**
		 * Schedules an asynchronous action
		 * @param targetPointer A pointer that contains the timestamp when 'f' should be triggered.
		 *                      May also contain None, in case 'f' is delayed indefinitely.
		 * @param f A function that's executed once 'targetPointer' contains a current or past timestamp. Yields a future.
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of 'f'
		 * @return A future that resolves once 'f' has been called and fully resolved.
		 */
		def variableFuture[A](targetPointer: Changing[Option[Instant]])(f: => Future[A])(implicit scheduler: Scheduler) =
			scheduler.scheduleVariable(targetPointer, executesOnJvmShutdown)(f)
	}
	
	case class AdvancedDelayFactory(lock: Option[AnyRef] = None, shutdownReaction: ShutdownReaction = Cancel)
	{
		/**
		 * @return A copy of this factory that executes the delayed action before allowing JVM to shut down.
		 *         The delay itself is skipped, however.
		 */
		def executingOnShutdown = withShutdownReaction(SkipDelay)
		/**
		 * @return A copy of this factory that executes the delayed action before allowing JVM to shut down.
		 *         Full delay will be applied.
		 */
		def delayingShutdown = withShutdownReaction(DelayShutdown)
		
		/**
		 * @param lock Wait lock to use
		 * @return A copy of this factory using the specified wait lock.
		 *         Said wait lock may be notified in order to skip the scheduled delay.
		 */
		def withWaitLock(lock: AnyRef) = copy(lock = Some(lock))
		/**
		 * @param reaction JVM shutdown reaction to apply
		 * @return A copy of this factory applying the specified JVM shutdown reaction
		 */
		def withShutdownReaction(reaction: ShutdownReaction) = copy(shutdownReaction = reaction)
		
		/**
		 * Schedules an asynchronous action
		 * @param target Target that describes when the action should be triggered
		 * @param f A function that's executed once 'target' is reached
		 * @param exc Implicit execution context
		 * @param log Implicit logging implementation
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of 'f'
		 * @return A future that resolves once 'f' has been called
		 */
		def apply[A](target: WaitTarget)(f: => A)(implicit exc: ExecutionContext, log: Logger, scheduler: Scheduler) =
			future(target) { Future.successful(f) }
		/**
		 * Schedules an asynchronous action
		 * @param target Target that describes when the action should be triggered
		 * @param f A function that's executed once 'target' is reached. Yields a future.
		 * @param exc Implicit execution context
		 * @param log Implicit logging implementation
		 * @param scheduler Implicit scheduler used when scheduling is deterministic
		 * @tparam A Result type of the future yielded by 'f'
		 * @return A future that resolves once 'f' has been called and fully resolved
		 */
		def future[A](target: WaitTarget)(f: => Future[A])
		             (implicit exc: ExecutionContext, log: Logger, scheduler: Scheduler): Future[A] =
		{
			if (target.isPossibleToReach) {
				// Case: More advanced conditions => Uses a DelayedProcess instance
				if ((lock.isDefined && target.breaksOnNotify) || shutdownReaction == DelayShutdown) {
					val promise = Promise[A]()
					val runFlag = Settable()
					val process = DelayedProcess(target, lock.getOrElse { new AnyRef }, Some(shutdownReaction)) { _ =>
						runFlag.set()
						Try(f) match {
							case Success(future) => promise.completeWith(future)
							case Failure(error) =>
								log(error, "Unexpected failure during a delayed event")
								promise.failure(error)
						}
					}
					process.runAsync()
					process.statePointer.once { _.isFinal } { _ =>
						if (runFlag.isNotSet)
							promise.failure(throw new InterruptedException("Process never completed"))
					}
					
					promise.future
				}
				// Case: Simple time-based condition => Uses the scheduler
				else
					target.endTime match {
						case Some(targetTime) =>
							val factory = {
								if (shutdownReaction == Cancel) simpleFactories.first else simpleFactories.second
							}
							factory.futureFrom(targetTime)(f)
							
						case None => Future.never
					}
			}
			// Case: Impossible target => Never resolves
			else
				Future.never
		}
	}
}
