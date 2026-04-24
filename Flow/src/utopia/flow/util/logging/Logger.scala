package utopia.flow.util.logging

import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.ScopeUsable
import utopia.flow.view.immutable.View

import scala.language.implicitConversions

object Logger
{
	// IMPLICIT    ------------------------
	
	/**
	  * Converts a function into a logger
	  * @param f Function to convert
	  * @return A new logger based on the specified function
	  */
	implicit def apply(f: (Option[Throwable], String, Model) => Unit): Logger = new LoggerFunction(f)
	
	implicit def scopeUsableLogger(l: Logger): ScopeUsable[Logger] = ScopeUsable(l)
	
	
	// OTHER    ---------------------------
	
	/**
	  * Creates a lazily initialized logger
	  * @param delegate A function that yields the logger. Called lazily, once the first log entry is required.
	  * @return A lazily initialized logger
	  */
	def delegateLazilyTo(delegate: => Logger) = DelegatingLogger(View(delegate))
	
	
	// NESTED   ---------------------------
	
	private class LoggerFunction(f: (Option[Throwable], String, Model) => Unit) extends Logger
	{
		override def apply(error: Option[Throwable], message: String, details: Model): Unit = f(error, message, details)
	}
}

/**
 * A common trait for logging interfaces
 * @author Mikko Hilpinen
 * @since 8.6.2022, v1.16
 */
trait Logger
{
	// ABSTRACT ---------------------------
	
	/**
	 * @param error Error to log, if applicable
	 * @param message Message to log, which may be empty
	 * @param details Additional details about this entry. May be empty.
	 */
	def apply(error: Option[Throwable], message: String, details: Model): Unit
	
	
	// OTHER    --------------------------
	
	/**
	 * @param error Error to log, if applicable
	 * @param message Message to log
	 */
	def apply(error: Option[Throwable], message: String): Unit = apply(error, message, Model.empty)
	/**
	 * @param error Error to log
	 * @param message Message to log
	 * @param details Additional details about this entry
	 */
	def apply(error: Throwable, message: String, details: Model): Unit = apply(Some(error), message, details)
	/**
	 * Logs an error
	 * @param error Error to log
	 * @param message Message to log along with the error
	 */
	def apply(error: Throwable, message: String): Unit = apply(Some(error), message)
	/**
	  * Logs an error
	  * @param error The error to log
	  */
	def apply(error: Throwable): Unit = apply(Some(error), "")
	/**
	 * @param message A message to log
	 * @param details Details to include
	 */
	def apply(message: String, details: Model): Unit = apply(None, message, details)
	/**
	 * Logs a (warning) message
	 * @param message Message to log
	 */
	def apply(message: String): Unit = apply(message, Model.empty)
	/**
	 * Logs details with no message
	 * @param details Details to log
	 */
	def apply(details: Model): Unit = apply("", details)
}
