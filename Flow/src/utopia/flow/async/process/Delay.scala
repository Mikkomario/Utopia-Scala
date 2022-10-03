package utopia.flow.async.process

import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.VolatileOption

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * An object used for performing delayed operations
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
object Delay
{
	// TODO: Create a variant that may delay further based on some condition (delay end time is pointer-based)
	
	/**
	  * Performs the specified function after a delay
	  * @param target           Wait target that determines the length of the delay
	  * @param lock             Wait lock to use (optional)
	  * @param shutdownReaction How this delay / process should handle situations where the JVM shuts down
	  *                         (default = skip function unless started already (if so, complete))
	  * @param f                Function to perform after the delay
	  * @param exc              Implicit execution context
	  * @tparam A Function return value
	  * @return Future that completes when the function has completed, containing the function's return value
	  */
	def apply[A](target: WaitTarget, lock: AnyRef = new AnyRef, shutdownReaction: ShutdownReaction = Cancel)
	            (f: => A)
	            (implicit exc: ExecutionContext, logger: Logger) =
	{
		if (target.isPositive) {
			val resultPointer = VolatileOption[Try[A]]()
			val process = DelayedProcess(target, lock, Some(shutdownReaction)) { _ => resultPointer.setOne(Try { f }) }
			process.runAsync()
			process.completionFuture.map { _ =>
				resultPointer.value.getOrElse { throw new InterruptedException("Process never completed") }.get
			}
		}
		// Case: May run immediately
		else
			Future(f)
	}
	
	
}