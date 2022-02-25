package utopia.flow.async

import utopia.flow.time.{Now, WaitTarget}
import utopia.flow.time.TimeExtensions._

import scala.concurrent.ExecutionContext
import scala.util.Try

object Wait
{
	/**
	  * Waits until the specified wait target is reached
	  * @param target Target for this wait
	  * @param lock Wait lock to use (optional)
	  * @param exc Implicit execution context
	  */
	def apply(target: WaitTarget, lock: AnyRef = new AnyRef)(implicit exc: ExecutionContext) =
	{
		if (target.isPositive)
			new Wait(target, lock).run()
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
	extends Process(lock)
{
	// IMPLEMENTED  ---------------------------
	
	override protected def runOnce() =
	{
		var waitCompleted = false
		target.endTime match {
			case Some(targetTime) =>
				lock.synchronized {
					while (!waitCompleted && targetTime.isInFuture) {
						val waitDuration = targetTime - Now
						// Performs the actual wait here (nano precision)
						// Exceptions are ignored
						Try {
							lock.wait(waitDuration.toMillis, waitDuration.getNano / 1000)
							// May break on any notify call. stop(), however, breaks regardless
							if (target.breaksOnNotify || state.isBroken)
								waitCompleted = true
						}
					}
				}
			case None =>
				lock.synchronized {
					while (!waitCompleted) {
						// Waits until notified, exceptions are ignored
						Try {
							lock.wait()
							waitCompleted = true
						}
					}
				}
		}
	}
}
