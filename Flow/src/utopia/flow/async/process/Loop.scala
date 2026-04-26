package utopia.flow.async.process

import utopia.flow.async.context.Scheduler
import utopia.flow.async.process.WaitTarget.{NoWait, WaitDuration, WeeklyTime}
import utopia.flow.time.{Duration, WeekDay}
import utopia.flow.view.mutable.async.Volatile

import java.time.{Instant, LocalTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Try}

object Loop
{
	// ATTRIBUTES   -------------------------
	
	val factory = new LoopFactory()
	
	
	// IMPLICIT -----------------------------
	
	// Implicitly converts this object into a Loop factory
	implicit def objectAsFactory(o: Loop.type): LoopFactory = o.factory
	
	
	// NESTED   ---------------------------
	
	class LoopFactory(initialWait: WaitTarget = NoWait)
	{
		/**
		 * @param initialWait Wait to apply before the first loop iteration
		 * @return A copy of this factory applying the specified delay
		 */
		def after(initialWait: WaitTarget) = new LoopFactory(initialWait)
		
		/**
		 * Starts a new loop that iterates once a day
		 * @param at Time of day, on which the iteration should be performed
		 * @param andImmediately Whether to trigger the first loop iteration immediately
		 *                       (or after the specified [[after]] target).
		 *                       Default = false.
		 * @param f A function to call on each loop iteration.
		 * @param scheduler Implicit scheduler used
		 * @return A future that resolves if the loop completes / breaks
		 */
		def daily[U](at: LocalTime, andImmediately: Boolean = false)(f: => U)(implicit scheduler: Scheduler) =
			_regularly(at, waitFirst = !andImmediately)(f)
		/**
		 * Starts a new loop that iterates once a week
		 * @param day Week day on which the iteration should be performed
		 * @param time Time of day, on which the iteration should be performed
		 * @param andImmediately Whether to trigger the first loop iteration immediately
		 *                       (or after the specified [[after]] target).
		 *                       Default = false.
		 * @param f A function to call on each loop iteration.
		 * @param scheduler Implicit scheduler used
		 * @return A future that resolves if the loop completes / breaks
		 */
		def weekly[U](day: WeekDay, time: LocalTime, andImmediately: Boolean = false)(f: => U)
		             (implicit scheduler: Scheduler) =
			_regularly(WeeklyTime(day, time), waitFirst = !andImmediately)(f)
		
		/**
		 * Starts a new loop that iterates between regular intervals
		 * @param interval Interval between iterations
		 * @param waitFirst Whether to wait 'interval' before the first loop iteration.
		 *                  NB: If set to true, will override any wait target specified in [[after]].
		 *                  Default = false.
		 * @param f A function to call on each loop iteration.
		 * @param scheduler Implicit scheduler used
		 * @return A future that resolves if the loop completes / breaks
		 */
		def regularly[U](interval: Duration, waitFirst: Boolean = false)(f: => U)(implicit scheduler: Scheduler) =
			_regularly(interval, waitFirst)(f)
		
		/**
		 * Starts a new loop
		 * @param f A function to call on each loop iteration. Yields the wait target for the next iteration.
		 * @param scheduler Implicit scheduler used
		 * @return A future that resolves if the loop completes / breaks
		 */
		def continuously(f: => WaitTarget)(implicit scheduler: Scheduler) = apply { Some(f) }
		
		/**
		 * Starts a new loop
		 * @param f A function to call on each loop iteration. Yields the wait target for the next iteration
		 *          (or None, if no more iterations should be performed).
		 * @param scheduler Implicit scheduler used
		 * @return A future that resolves once / if the loop completes
		 */
		def apply(f: => Option[WaitTarget])(implicit scheduler: Scheduler) =
			initialWait.endTime match {
				case Some(initialTarget) => _apply(initialTarget)(f)
				case None => Future.unit
			}
		/**
		 * Starts a new loop
		 * @param f A function to call on each loop iteration.
		 *          Yields a future that yields the wait target for the next iteration
		 *          (or None, if no more iterations should be performed)
		 * @param scheduler Implicit scheduler used
		 * @param exc Implicit execution context
		 * @return A future that resolves once / if the loop completes
		 */
		def async(f: => Future[Option[WaitTarget]])(implicit scheduler: Scheduler, exc: ExecutionContext) =
			initialWait.endTime match {
				case Some(initialTarget) => _async(initialTarget)(f)
				case None => Future.unit
			}
		
		/**
		 * Performs the specified operation repeatedly until it succeeds
		 * or until a specific amount of attempts has been made
		 * @param firstInterval Interval between the first and the second attempt
		 * @param maxAttempts Maximum number of attempts to make (> 0)
		 * @param intervalModifier A modifier applied to the interval between consecutive attempts
		 *                         (default = 1.0 = not modified)
		 * @param f Attempt function that returns a success or a failure
		 * @param exc Implicit execution context
		 * @tparam A Type of successful function result
		 * @return A future that completes when the specified function completes successfully or when enough failed
		 *         attempts have been made (contains success or failure, accordingly)
		 */
		def tryRepeatedly[A](firstInterval: Duration, maxAttempts: Int, intervalModifier: Double = 1.0)(f: => Try[A])
		                    (implicit exc: ExecutionContext, scheduler: Scheduler) =
		{
			// The delay between attempts may be modified between attempts
			val intervalIterator = {
				if (intervalModifier == 1.0)
					Iterator.continually(firstInterval)
				else
					Iterator.iterate(firstInterval: Duration) { _ * intervalModifier }
			}.take(maxAttempts - 1)
			// Stores the function return value(s) to a pointer
			val resultPointer = Volatile.optional[Try[A]]()
			// Starts looping in the background
			this
				.apply {
					val result = Try(f).flatten
					resultPointer.setOne(result)
					if (result.isSuccess)
						None
					else
						intervalIterator.nextOption().map { WaitDuration(_) }
				}
				// Returns a future that completes when the loop closes
				.map { _ => resultPointer.value.getOrElse { Failure(new InterruptedException("No attempts")) } }
		}
		
		private def _regularly[U](interval: WaitTarget, waitFirst: Boolean = false)(f: => U)
		                         (implicit scheduler: Scheduler) =
		{
			val factory = if (waitFirst) after(interval) else this
			factory.continuously { f; interval }
		}
		
		private def _apply(target: Instant)(f: => Option[WaitTarget])(implicit scheduler: Scheduler): Future[Unit] =
			scheduler.schedule(target) {
				f.flatMap { _.endTime } match {
					case Some(nextTarget) => _apply(nextTarget)(f)
					case None => Future.unit
				}
			}
		private def _async(target: Instant)(f: => Future[Option[WaitTarget]])
		                  (implicit scheduler: Scheduler, exc: ExecutionContext): Future[Unit] =
			scheduler.schedule(target) {
				f.flatMap { nextTarget =>
					nextTarget.flatMap { _.endTime } match {
						case Some(nextTarget) => _async(nextTarget)(f)
						case None => Future.unit
					}
				}
			}
	}
}