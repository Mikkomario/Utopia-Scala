package utopia.flow.async

import utopia.flow.util.WaitTarget

import scala.concurrent.{ExecutionContext, Promise}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

object TryLoop
{
	/**
	 * Calls the specified function possibly multiple times until a successful result is received or maximum attempts
	 * limit is reached
	 * @param durationBetweenAttempts Wait duration between each attempt
	 * @param maxAttempts Maximum number of attempts in total
	 * @param operation Operation to be run (may be called multiple times)
	 * @param exc Implicit execution context
	 * @tparam A Type of operation result
	 * @return Asynchronous future of the final operation result (contains a failure if max attempts was reached)
	 */
	def attempt[A](durationBetweenAttempts: Duration, maxAttempts: Int)(operation: Try[A])(implicit exc: ExecutionContext) =
	{
		val loop = new TryLoop(durationBetweenAttempts, maxAttempts)(operation)
		loop.registerToStopOnceJVMCloses()
		loop.startAsync()
		loop.future
	}
}

/**
 * Used for attempting a failure-prone task possibly multiple times in a loop
 * @author Mikko Hilpinen
 * @since 10.3.2020, v1.6.1
 */
class TryLoop[A](val durationBetweenAttempts: Duration, maxAttempts: Int)(operation: => Try[A])
	extends Loop
{
	// ATTRIBUTES   -------------------------
	
	private val promise: Promise[Try[A]] = Promise()
	private var failures = 0
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return Future of the eventual result of this loop
	 */
	def future = promise.future
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def runOnce() =
	{
		if (!promise.isCompleted)
		{
			// Performs the operation
			val result = operation
			if (result.isSuccess)
			{
				promise.success(result)
				stop()
			}
			else
			{
				// May break on failure, if maximum attempt limit is reached
				failures += 1
				if (failures >= maxAttempts)
				{
					promise.success(result)
					stop()
				}
			}
		}
	}
	
	override def stop() =
	{
		if (!promise.isCompleted)
			promise.success(Failure(
				new InterruptedException(s"TryLoop was stopped before a successful result could be generated (previous failures: $failures)")))
		super.stop()
	}
	
	override protected val nextWaitTarget = WaitTarget.WaitDuration(durationBetweenAttempts)
}
