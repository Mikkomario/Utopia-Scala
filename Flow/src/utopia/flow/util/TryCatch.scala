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
	 * @return This TryCatch converted to a Try. Removes non-critical error data.
	 */
	def toTry: Try[A]
	/**
	 * @return Successful value acquired, if applicable.
	 */
	def success: Option[A]
	/**
	 * @return Any single failure encountered. May be critical or non-critical.
	 */
	def anyFailure: Option[Throwable]
	/**
	 * @return All failures encountered. May be critical or non-critical.
	 */
	def failures: Vector[Throwable]
	
	/**
	 * Logs non-critical failures that were encountered and returns a Try
	 * @param log Implicit logging implementation for encountered non-critical failures
	 * @return Success if this operation was at least partially successful, Failure otherwise
	 */
	def logToTry(implicit log: Logger): Try[A]
}

object TryCatch
{
	// IMPLICIT -------------------------
	
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
		def logFailures(implicit log: Logger) = failures.headOption.foreach { error =>
			log(error, s"Encountered ${failures.size} non-critical errors")
		}
		
		
		// IMPLEMENTED  ------------------------
		
		override def toTry = scala.util.Success(value)
		
		override def success = Some(value)
		override def anyFailure: Option[Throwable] = failures.headOption
		
		override def logToTry(implicit log: Logger) = {
			logFailures
			toTry
		}
	}
	/**
	 * Represents a failed attempt
	 * @param cause Cause of this failure
	 * @tparam A Type of acquired result, when successful
	 */
	case class Failure[+A](cause: Throwable) extends TryCatch[A]
	{
		override def toTry = scala.util.Failure[A](cause)
		
		override def success = None
		override def anyFailure = Some(cause)
		override def failures: Vector[Throwable] = Vector(cause)
		
		override def logToTry(implicit log: Logger) = toTry
	}
}