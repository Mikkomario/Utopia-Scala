package utopia.flow.util

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.util.logging.Logger
import StringExtensions._

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
	def failures: Seq[Throwable]
	/**
	 * @return Encountered non-critical failures.
	 */
	def partialFailures: Seq[Throwable]
	
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
	def log(implicit log: Logger): Option[A]
	/**
	  * Logs critical and non-critical failures that were encountered and returns an Option
	  * @param message Message to add to the logging entry
	  * @param log Implicit logging implementation
	  * @return Some on success, None on failure
	  */
	def logWithMessage(message: => String)(implicit log: Logger): Option[A]
	
	/**
	  * @param backup A value to use as a backup if this TryCatch is a failure
	  * @tparam B Type of the backup success value
	  * @return If this is a success, returns this, otherwise returns the backup value
	  */
	def orElse[B >: A](backup: => TryCatch[B]): TryCatch[B]
	
	/**
	  * @param f A mapping function to apply to a successful value, if available
	  * @tparam B Type of the mapping function's result
	  * @return Copy of this result where a successful value has been mapped, if applicable
	  */
	def map[B](f: A => B): TryCatch[B]
	/**
	  * @param f A mapping function to apply to a successful value, if available.
	  *          Yields a TryCatch.
	  * @tparam B Type of the mapping function's result
	  * @return Copy of this result where a successful value has been mapped, if applicable.
	  *         May map from success into a failure.
	  */
	def flatMap[B](f: A => TryCatch[B]): TryCatch[B]
	/**
	  * @param f A mapping function to apply to a successful value, if available. Yields a Try.
	  * @tparam B Type of the mapping function's result
	  * @return Copy of this result where a successful value has been mapped, if applicable.
	  *         May map from success into a failure.
	  */
	def tryMap[B](f: A => Try[B]): TryCatch[B]
	
	/**
	 * @param failures Additional non-critical failures to assign to this item (call-by-name)
	 * @return Copy of this item with additional failures included (if appropriate)
	 */
	def withAdditionalFailures(failures: => IterableOnce[Throwable]): TryCatch[A]
	
	
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
	
	@deprecated("Renamed to .log", "v2.5.1")
	def logToOption(implicit log: Logger): Option[A] = this.log
	
	
	// OTHER    --------------------------
	
	@deprecated("Renamed to .logWithMessage", "v2.5.1")
	def logToOptionWithMessage(message: => String)(implicit log: Logger): Option[A] = logWithMessage(message)
}

object TryCatch
{
	// IMPLICIT -------------------------
	
	implicit def fromTry[A](t: Try[(A, IndexedSeq[Throwable])]): TryCatch[A] = t match {
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
	def apply[A](f: => (A, IndexedSeq[Throwable])): TryCatch[A] = Try(f) match {
		case scala.util.Success((value, failures)) => Success(value, failures)
		case scala.util.Failure(error) => Failure(error)
	}
	
	/**
	  * Wraps / converts a regular try into a TryCatch
	  * @param t Try to convert
	  * @tparam A Type of wrapped success value
	  * @return A TryCatch representing the specified Try
	  */
	def wrap[A](t: Try[A]): TryCatch[A] = t match {
		case scala.util.Success(v) => Success(v)
		case scala.util.Failure(error) => Failure(error)
	}
	
	
	// NESTED   -------------------------
	
	/**
	 * Represents a successful attempt that may have partially failed
	 * @param value The successful result acquired
	 * @param failures Non-critical failures encountered during the process
	 * @tparam A Type of acquired result
	 */
	case class Success[+A](value: A, failures: Seq[Throwable] = Empty) extends TryCatch[A]
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
		override def partialFailures: Seq[Throwable] = failures
		
		override def logToTry(implicit log: Logger) = {
			logFailures()
			toTry
		}
		override def log(implicit log: Logger) = {
			logFailures()
			Some(value)
		}
		override def logToTryWithMessage(message: => String)(implicit log: Logger): Try[A] = {
			logFailures(message)
			toTry
		}
		override def logWithMessage(message: => String)(implicit log: Logger): Option[A] = {
			logFailures(message)
			Some(value)
		}
		
		override def orElse[B >: A](backup: => TryCatch[B]): TryCatch[B] = this
		
		override def map[B](f: A => B): TryCatch[B] = Success(f(value), failures)
		override def flatMap[B](f: A => TryCatch[B]): TryCatch[B] = {
			val mapResult = f(value)
			if (failures.isEmpty)
				mapResult
			else
				mapResult match {
					case Success(newVal, newFailures) => Success(newVal, failures ++ newFailures)
					case f: Failure[B] => f
				}
		}
		override def tryMap[B](f: A => Try[B]): TryCatch[B] = {
			f(value) match {
				case scala.util.Success(v) => copy(v)
				case scala.util.Failure(error) => Failure(error)
			}
		}
		
		override def withAdditionalFailures(failures: => IterableOnce[Throwable]): TryCatch[A] =
			copy(failures = this.failures ++ failures)
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
		override def failures: IndexedSeq[Throwable] = Single(cause)
		override def partialFailures: IndexedSeq[Throwable] = Empty
		
		override def logToTry(implicit log: Logger) = toTry
		override def log(implicit log: Logger) = {
			log(cause)
			None
		}
		override def logToTryWithMessage(message: => String)(implicit log: Logger): Try[A] = toTry
		override def logWithMessage(message: => String)(implicit log: Logger): Option[A] = {
			log(cause, message)
			None
		}
		
		override def orElse[B >: A](backup: => TryCatch[B]): TryCatch[B] = backup
		
		override def map[B](f: A => B): TryCatch[B] = Failure(cause)
		override def flatMap[B](f: A => TryCatch[B]): TryCatch[B] = Failure(cause)
		override def tryMap[B](f: A => Try[B]): TryCatch[B] = Failure(cause)
		
		override def withAdditionalFailures(failures: => IterableOnce[Throwable]): TryCatch[A] = this
	}
}