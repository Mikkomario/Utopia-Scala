package utopia.vault.util

import utopia.flow.util.logging.Logger

import scala.language.implicitConversions

/**
  * Different error handling styles extend this trait and can be used when handling various errors (in vault)
  * @author Mikko Hilpinen
  * @since 18.7.2019, v1.3+
  */
trait ErrorHandlingPrinciple
{
	/**
	  * Handles the error according to this principle. May throw.
	  * @param error The error to be handled
	  */
	def handle(error: Throwable): Unit
}

object ErrorHandlingPrinciple
{
	/**
	  * Ignores all errors silently, which may cause unpredictable behavior
	  */
	case object Ignore extends ErrorHandlingPrinciple { def handle(error: Throwable) = () }
	
	/**
	  * Throws all errors, which is likely to cause the program to crash
	  */
	case object Throw extends ErrorHandlingPrinciple { def handle(error: Throwable) = throw error }
	
	object Log
	{
		/**
		  * Creates a new error handling principle that utilizes a logger
		  * @param logger The logger to use
		  * @return A new principle using that logger
		  */
		def using(logger: Logger) = new Log()(logger)
		
		implicit def objectToInstance(o: Log.type)(implicit logger: Logger): Log = o.using(logger)
	}
	/**
	  * Logs all encountered errors using the specified logging implementation
	  * @param logger A logging implementation to use
	  */
	class Log()(implicit logger: Logger) extends  ErrorHandlingPrinciple {
		override def handle(error: Throwable) = logger(error)
	}
	
	object Custom
	{
		def apply[U](handle: Throwable => U) = new Custom(handle)
	}
	/**
	  * Handles all errors with a specific function. May be used for logging, for example.
	  * @param handler A handling function that will receive caught Throwables
	  */
	class Custom[U](handler: Throwable => U)
		extends ErrorHandlingPrinciple { def handle(error: Throwable) = handler(error) }
}