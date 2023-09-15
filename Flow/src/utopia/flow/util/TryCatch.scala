package utopia.flow.util

import utopia.flow.util.logging.Logger

import scala.language.implicitConversions
import scala.util.Try

/**
 * Used for returning values from processes that can fail either completely or non-critically
 * @author Mikko Hilpinen
 * @since 2.5.2023, v2.2
 * @tparam A Type of acquired result, when successful
 */
sealed trait TryCatch[+A]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return The successful value in this TryCatch.
	 *         Throws if this is a failure.
	 */
	def get: A
	
	/**
	 * @return Whether this is a full or partial success
	 */
	def isSuccess: Boolean
	
	/**
	 * @return This TryCatch converted to a Try. Removes non-critical error data.
	 */
	def toTry: Try[A]
	/**
	 * @return Successful value acquired, if applicable.
	 */
	def success: Option[A]
	/**
	 * @return Critical failure that was encountered. None if no critical failures were encountered.
	 */
	def failure: Option[Throwable]
	/**
	 * @return Any single failure encountered. May be critical or non-critical.
	 */
	def anyFailure: Option[Throwable]
	/**
	 * @return All failures encountered. May be critical or non-critical.
	 */
	def failures: Vector[Throwable]
	/**
	 * @return Encountered non-critical failures.
	 */
	def partialFailures: Vector[Throwable]
	
	/**
	 * Logs non-critical failures that were encountered and returns a Try
	 * @param log Implicit logging implementation for encountered non-critical failures
	 * @return Success if this operation was at least partially successful, Failure otherwise
	 */
	def logToTry(implicit log: Logger): Try[A]
	/**
	  * Logs non-critical failures that were encountered and returns a Try
	  * @param message Message to add to the logging entry (call-by-name)
	  * @param log Implicit logging implementation
	  * @return Success if this operation was at least partially successful, Failure otherwise
	  */
	def logToTryWithMessage(message: => String)(implicit log: Logger): Try[A]
	/**
	 * Logs critical and non-critical failures that were encountered and returns an Option
	 * @param log Implicit logging implementation
	 * @return Some on success, None on failure
	 */
	def logToOption(implicit log: Logger): Option[A]
	/**
	  * Logs critical and non-critical failures that were encountered and returns an Option
	  * @param message Message to add to the logging entry
	  * @param log Implicit logging implementation
	  * @return Some on success, None on failure
	  */
	def logToOptionWithMessage(message: => String)(implicit log: Logger): Option[A]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether this is a complete failure
	 */
	def isFailure = !isSuccess
	
	/**
	 * @return A try based on this item that doesn't contain partial failure information +
	 *         possible partial failures as a separate collection
	 */
	def separateToTry = toTry -> partialFailures
}

object TryCatch
{
	// IMPLICIT -------------------------
	
	implicit def fromTry[A](t: Try[(A, Vector[Throwable])]): TryCatch[A] = t match {
		case scala.util.Success((result, failures)) => Success(result, failures)
		case scala.util.Failure(error) => Failure(error)
	}
	implicit def convertFailure[A](f: scala.util.Failure[A]): Failure[A] = Failure(f.exception)
	
	
	// OTHER    -------------------------
	
	/**
	 * Performs the specified operation, catching possible failures
	 * @param f A function to perform. Returns a value, as well as non-critical failures that were encountered.
	 * @tparam A Type of successful value returned
	 * @return Success or failure based on whether the specified function threw
	 */
	def apply[A](f: => (A, Vector[Throwable])): TryCatch[A] = Try(f) match {
		case scala.util.Success((value, failures)) => Success(value, failures)
		case scala.util.Failure(error) => Failure(error)
	}
	
	
	// NESTED   -------------------------
	
	/**
	 * Represents a successful attempt that may have partially failed
	 * @param value The successful result acquired
	 * @param failures Non-critical failures encountered during the process
	 * @tparam A Type of acquired result
	 */
	case class Success[+A](value: A, failures: Vector[Throwable] = Vector()) extends TryCatch[A]
	{
		// COMPUTED ----------------------------
		
		/**
		 * Logs the non-critical failures that were encountered
		 * @param log Implicit logging implementation
		 */
		def logFailures(message: => String = s"Encountered ${failures.size} non-critical errors")(implicit log: Logger) =
			failures.headOption.foreach { log(_, message) }
		
		
		// IMPLEMENTED  ------------------------
		
		override def isSuccess: Boolean = true
		
		override def get: A = value
		override def toTry = scala.util.Success(value)
		
		override def success = Some(value)
		override def failure: Option[Throwable] = None
		override def anyFailure: Option[Throwable] = failures.headOption
		override def partialFailures: Vector[Throwable] = failures
		
		override def logToTry(implicit log: Logger) = {
			logFailures()
			toTry
		}
		override def logToOption(implicit log: Logger) = {
			logFailures()
			Some(value)
		}
		override def logToTryWithMessage(message: => String)(implicit log: Logger): Try[A] = {
			logFailures(message)
			toTry
		}
		override def logToOptionWithMessage(message: => String)(implicit log: Logger): Option[A] = {
			logFailures(message)
			Some(value)
		}
	}
	/**
	 * Represents a failed attempt
	 * @param cause Cause of this failure
	 * @tparam A Type of acquired result, when successful
	 */
	case class Failure[+A](cause: Throwable) extends TryCatch[A]
	{
		override def isSuccess: Boolean = false
		
		override def get: A = throw cause
		override def toTry = scala.util.Failure[A](cause)
		
		override def success = None
		override def failure: Option[Throwable] = Some(cause)
		override def anyFailure = Some(cause)
		override def failures: Vector[Throwable] = Vector(cause)
		override def partialFailures: Vector[Throwable] = Vector.empty
		
		override def logToTry(implicit log: Logger) = toTry
		override def logToOption(implicit log: Logger) = {
			log(cause)
			None
		}
		override def logToTryWithMessage(message: => String)(implicit log: Logger): Try[A] = toTry
		override def logToOptionWithMessage(message: => String)(implicit log: Logger): Option[A] = {
			log(cause, message)
			None
		}
	}
}