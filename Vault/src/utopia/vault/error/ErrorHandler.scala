package utopia.vault.error

import utopia.flow.util.logging.Logger

import scala.language.implicitConversions

object ErrorHandler
{
	// COMPUTED -------------------------
	
	/**
	  * @param logger Implicit logging implementation
	  * @return An error handler that delegates the errors to the specified logger
	  */
	def log(implicit logger: Logger): ErrorHandler = logUsing(logger)
	
	
	// IMPLICIT -------------------------
	
	/**
	  * @param logger A logging implementation
	  * @return An error handler that delegates the errors to the specified logger
	  */
	implicit def logUsing(logger: Logger): ErrorHandler = new LogUsing(logger)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A function that handles received errors
	  * @tparam U Arbitrary function result type
	  * @return An error handler that uses the specified function to handle all errors
	  */
	def apply[U](f: Throwable => U): ErrorHandler = new _ErrorHandler(f)
	
	
	// NESTED   -------------------------
	
	/**
	  * Rethrows all encountered errors
	  */
	object Rethrow extends ErrorHandler
	{
		override def apply(error: Throwable): Unit = throw error
	}
	/**
	  * Silently ignores all errors
	  */
	object Ignore extends ErrorHandler
	{
		override def apply(error: Throwable): Unit = ()
	}
	
	/**
	  * Logs all errors using the specified logger
	  * @param log A logging implementation to use
	  */
	class LogUsing(log: Logger) extends ErrorHandler
	{
		override def apply(error: Throwable): Unit = log(error)
	}
	
	private class _ErrorHandler[U](f: Throwable => U) extends ErrorHandler
	{
		override def apply(error: Throwable): Unit = f(error)
	}
}

/**
  * Common trait for interfaces which handle errors
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
trait ErrorHandler
{
	/**
	  * Handles the specified error. May rethrow.
	  * @param error The error to handle
	  */
	def apply(error: Throwable): Unit
}
