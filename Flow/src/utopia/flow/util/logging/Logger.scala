package utopia.flow.util.logging

import scala.language.implicitConversions

object Logger
{
	// OTHER    ---------------------------
	
	/**
	  * Converts a function into a logger
	  * @param f Function to convert
	  * @return A new logger based on the specified function
	  */
	implicit def apply(f: (Option[Throwable], String) => Unit): Logger = new LoggerFunction(f)
	
	
	// NESTED   ---------------------------
	
	private class LoggerFunction(f: (Option[Throwable], String) => Unit) extends Logger
	{
		override def apply(error: Option[Throwable], message: String) = f(error, message)
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
	 */
	def apply(error: Option[Throwable], message: String): Unit
	
	
	// OTHER    --------------------------
	
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
	 * Logs a (warning) message
	 * @param message Message to log
	 */
	def apply(message: String): Unit = apply(None, message)
}
