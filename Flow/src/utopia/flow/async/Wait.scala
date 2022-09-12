package utopia.flow.async

import utopia.flow.time.{Now, WaitTarget}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.SysErrLogger
import utopia.flow.util.CollectionExtensions._

import scala.concurrent.ExecutionContext
import scala.util.Try

object Wait
{
	/**
	  * Waits until the specified wait target is reached
	  * @param target Target for this wait
	  * @param lock Wait lock to use (optional)
	  * @param exc Implicit execution context
	  * @return Whether the wait completed without being interrupted through an InterruptedException.
	  *         If false, the process that follows should likewise take measures to hurry to its completion
	  *         (use discrection).
	  */
	def apply(target: WaitTarget, lock: AnyRef = new AnyRef)(implicit exc: ExecutionContext) = {
		if (target.isPositive) {
			val wait = new Wait(target, lock)
			wait.run()
			wait.state.isNotBroken
		}
		else
			true
	}
}

/**
  * Represents a single active wait period
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
class Wait(val target: WaitTarget, val lock: AnyRef = new AnyRef,
           override val shutdownReaction: Option[ShutdownReaction] = None, override val isRestartable: Boolean = true)
          (implicit exc: ExecutionContext)
	extends Process(lock)(exc, SysErrLogger)
{
	// IMPLEMENTED  ---------------------------
	
	override protected def runOnce() =
	{
		// Catches InterruptedExceptions
		Try {
			var waitCompleted = false
			target.endTime match {
				case Some(targetTime) =>
					lock.synchronized {
						while (!waitCompleted && targetTime.isInFuture) {
							val waitDuration = targetTime - Now
							// Performs the actual wait here (nano precision)
							lock.wait(waitDuration.toMillis, waitDuration.getNano / 1000)
							// May break on any notify call. Also, stop() always breaks
							if (target.breaksOnNotify || state.isBroken)
								waitCompleted = true
						}
					}
				// Waits until notified
				case None => lock.synchronized { lock.wait() }
			}
		}.failure.foreach {
			// Case: Interrupted during waiting => Treats as if broken through stop()
			case _: InterruptedException => markAsInterrupted()
			// Case: Interrupted through another exception => Logs
			case error => SysErrLogger(error, "Unexpected error during a Wait")
		}
	}
}
