package utopia.flow.async.process

import scala.concurrent.{ExecutionContext, Future}
import scala.sys.process.Process
import scala.util.{Failure, Success}
import utopia.flow.util.StringExtensions._

/**
 * Wraps a [[scala.sys.process.Process]], offering a few utility functions
 * @param process The process to wrap
 * @param readOut The captured stdout & stderr content (call-by-name). Default = empty.
 * @param readErr The captured stderr content (call-by-name). Default = empty.
 * @author Mikko Hilpinen
 * @since 02.03.2026, v2.8
 */
class AsyncSystemProcess(process: Process, readOut: => String = "", readErr: => String = "")
                        (implicit exc: ExecutionContext)
	extends Breakable
{
	// ATTRIBUTES   -------------------------
	
	/**
	 * A future that resolves once this process completes.
	 * Includes the captured output, if applicable.
	 */
	val future = Future {
		val exitCode = process.exitValue()
		if (exitCode == 0)
			Success(output)
		else
			Failure(new IllegalStateException(s"Process exited with code $exitCode${
				err.prependIfNotEmpty("\nErr: ") }"))
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
	
	/**
	 * @return The (stdout + stderr) output of the wrapped process, if captured
	 */
	def output = readOut
	/**
	 * @return The stderr output of the wrapped process, if captured
	 */
	def err = readErr
	
	
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
