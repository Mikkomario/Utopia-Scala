package utopia.flow.async.process

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.Process
import scala.util.{Failure, Success}

/**
 * Wraps a scala.sys.process.Process, offering a few utility functions
 * @author Mikko Hilpinen
 * @since 02.03.2026, v2.8
 */
class AsyncSystemProcess(process: Process)(implicit exc: ExecutionContext) extends Breakable
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * A future that resolves once this process completes
	 */
	val future = Future {
		val exitCode = process.exitValue()
		if (exitCode == 0)
			Success(())
		else
			Failure(new IllegalStateException(s"Process exited with code $exitCode"))
	}
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return Whether this process has already completed
	 */
	def isCompleted = future.isCompleted
	/**
	 * @return Whether this process is yet to complete
	 */
	def isPending = !isCompleted
	
	
	// IMPLEMENTED  -------------------------
	
	override def stop(): Future[Any] = {
		if (isCompleted)
			Future.unit
		else {
			process.destroy()
			future
		}
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * Kills this process, if it hasn't completed yet
	 */
	def kill() = {
		if (isPending)
			process.destroy()
	}
	
	/**
	 * @param timeoutFuture A future that resolves once this process should time out / forcibly terminate
	 * @return This process
	 */
	def timeoutWith(timeoutFuture: Future[_]) = {
		if (timeoutFuture.isCompleted)
			kill()
		else if (isPending)
			timeoutFuture.onComplete { _ => kill() }
		this
	}
}
