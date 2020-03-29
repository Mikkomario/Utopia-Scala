package utopia.vault.util

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
	case object Ignore extends ErrorHandlingPrinciple { def handle(error: Throwable) = Unit }
	
	/**
	  * Throws all errors, which is likely to cause the program to crash
	  */
	case object Throw extends ErrorHandlingPrinciple { def handle(error: Throwable) = throw error }
	
	/**
	  * Handles all errors with a specific function. May be used for logging, for example.
	  * @param handler A handling function that will receive caught Throwables
	  */
	case class Custom(handler: Throwable => Unit) extends ErrorHandlingPrinciple { def handle(error: Throwable) = handler(error) }
}