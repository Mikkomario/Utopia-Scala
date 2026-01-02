package utopia.flow.util.result

import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.flow.collection.mutable.builder.TryCatchBuilder
import utopia.flow.util.logging.Logger

import scala.language.implicitConversions
import scala.util.Try

/**
 * Used for returning values from processes that can fail either completely or non-critically
 * @author Mikko Hilpinen
 * @since 2.5.2023, v2.2
 * @tparam A Type of acquired result, when successful
 */
sealed trait TryCatch[+A] extends MayHaveFailed[A] with MayHaveFailedLike[A, TryCatch, TryCatch, TryCatch]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Whether this is a partial success and a partial failure, both
	 */
	def isPartialFailure: Boolean
	
	/**
	 * @return Either:
	 *              - Right: Successfully acquired value, plus partial failures
	 *              - Left: Failure cause
	 */
	def toEither: Either[Throwable, (A, Seq[Throwable])]
	
	/**
	 * @return Any single failure encountered. May be critical or non-critical.
	 */
	def anyFailure: Option[Throwable]
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
	  * @param f A mapping function to apply to a successful value, if available.
	  *          Yields a TryCatch.
	  * @tparam B Type of the mapping function's result
	  * @return Copy of this result where a successful value has been mapped, if applicable.
	  *         May map from success into a failure.
	  */
	def flatMap[B](f: A => TryCatch[B]): TryCatch[B]
	/**
	 * @param f A mapping function applied to a successful value, if applicable.
	 *          May yield failures, that will be considered partial.
	 * @tparam B Mapping result type
	 * @return Copy of this result where the successful value is mapped, if applicable.
	 *         Failures from 'f' are collected and treated as partial failures, if appropriate.
	 */
	def mapCatching[B](f: A => (B, IterableOnce[Throwable])): TryCatch[B]
	
	/**
	 * @param failures Additional non-critical failures to assign to this item (call-by-name)
	 * @return Copy of this item with additional failures included (if appropriate)
	 */
	def withAdditionalFailures(failures: => IterableOnce[Throwable]): TryCatch[A]
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return A try based on this item that doesn't contain partial failure information +
	 *         possible partial failures as a separate collection
	 */
	def separateToTry = toTry -> partialFailures
	
	@deprecated("Renamed to .log", "v2.5.1")
	def logToOption(implicit log: Logger): Option[A] = this.log
	
	
	// IMPLEMENTED  ----------------------
	
	override def toTryCatch: TryCatch[A] = this
	
	override def catching[B >: A](partialFailures: => IterableOnce[Throwable]): TryCatch[B] =
		withAdditionalFailures(partialFailures)
	
	override def tryMapCatching[B](f: A => TryCatch[B]): TryCatch[B] = flatMap(f)
	
	
	// OTHER    --------------------------
	
	@deprecated("Renamed to .logWithMessage", "v2.5.1")
	def logToOptionWithMessage(message: => String)(implicit log: Logger): Option[A] = logWithMessage(message)
}

object TryCatch
{
	// COMPUTED -------------------------
	
	/**
	 * @return Access to constructing new TryCatch-builders
	 */
	def builder = TryCatchBuilder
	
	
	// IMPLICIT -------------------------
	
	implicit def fromTry[A](t: Try[(A, IndexedSeq[Throwable])]): TryCatch[A] = t match {
		case scala.util.Success((result, failures)) => Success(result, failures)
		case scala.util.Failure(error) => Failure(error)
	}
	implicit def convertFailure[A](f: scala.util.Failure[A]): Failure = Failure(f.exception)
	
	
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
	
	
	// VALUES   -------------------------
	
	/**
	 * Represents a successful attempt that may have partially failed
	 * @param value The successful result acquired
	 * @param failures Non-critical failures encountered during the process
	 * @tparam A Type of acquired result
	 */
	case class Success[+A](value: A, override val failures: Seq[Throwable] = Empty) extends TryCatch[A]
	{
		// COMPUTED ----------------------------
		
		/**
		 * Logs the non-critical failures that were encountered
		 * @param log Implicit logging implementation
		 */
		def logFailures(message: => String = s"Encountered ${failures.size} non-critical errors")
		               (implicit log: Logger) =
			failures.headOption.foreach { log(_, message) }
		
		
		// IMPLEMENTED  ------------------------
		
		override def isSuccess: Boolean = true
		override def isFailure: Boolean = false
		override def isPartialFailure: Boolean = failures.nonEmpty
		
		override def get: A = value
		override def toTry = scala.util.Success(value)
		override def toEither: Either[Throwable, (A, Seq[Throwable])] = Right(value -> failures)
		
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
					case f: Failure => f
				}
		}
		override def tryMap[B](f: A => Try[B]): TryCatch[B] = {
			f(value) match {
				case scala.util.Success(v) => copy(v)
				case scala.util.Failure(error) => Failure(error)
			}
		}
		override def mapCatching[B](f: A => (B, IterableOnce[Throwable])): TryCatch[B] = {
			val (result, newFailures) = f(value)
			Success(result, failures ++ newFailures)
		}
		override def mapOrFail[B](f: A => MayHaveFailed[B]): TryCatch[B] = f(value).toTryCatch
		
		override def withAdditionalFailures(failures: => IterableOnce[Throwable]): TryCatch[A] =
			copy(failures = this.failures ++ failures)
	}
	/**
	 * Represents a failed attempt
	 * @param cause Cause of this failure
	 */
	case class Failure(override val cause: Throwable)
		extends MayHaveFailed.Failure with MayHaveFailed.FailureLike[Failure] with TryCatch[Nothing]
	{
		override def self: Failure = this
		
		override def isPartialFailure: Boolean = false
		
		override def anyFailure = Some(cause)
		override def failures: IndexedSeq[Throwable] = Single(cause)
		override def partialFailures: IndexedSeq[Throwable] = Empty
		
		override def toTryCatch = this
		override def toEither: Either[Throwable, (Nothing, Seq[Throwable])] = Left(cause)
		
		override def logToTry(implicit log: Logger) = toTry
		override def log(implicit log: Logger) = {
			log(cause)
			None
		}
		override def catching[B >: Nothing](partialFailures: => IterableOnce[Throwable]) = this
		override def withAdditionalFailures(failures: => IterableOnce[Throwable]): TryCatch[Nothing] = this
		
		override def orElse[B >: Nothing](backup: => TryCatch[B]): TryCatch[B] = backup
		
		override def flatMap[B](f: Nothing => TryCatch[B]): TryCatch[B] = this
		override def mapCatching[B](f: Nothing => (B, IterableOnce[Throwable])): TryCatch[B] = this
		
		override def logToTryWithMessage(message: => String)(implicit log: Logger) = toTry
		override def logWithMessage(message: => String)(implicit log: Logger) = {
			log(cause, message)
			None
		}
	}
	
	
	// EXTENSIONS   ----------------------------
	
	implicit class TryCatchMany[A](val t: TryCatch[Iterable[A]]) extends AnyVal
	{
		/**
		 * @param other Another attempt
		 * @tparam B Type of individual results in the other attempt
		 * @return A combined version of these two attempts,
		 *         which is a success if either of these attempts succeeded.
		 *         Contains the combined values and/or failures from both attempts.
		 */
		def ++[B >: A](other: TryCatch[Iterable[B]]): TryCatch[IndexedSeq[B]] = t match {
			case Success(coll1, failures1) =>
				other match {
					case Success(coll2, failures2) =>
						Success(OptimizedIndexedSeq.concat(coll1, coll2), failures1 ++ failures2)
					case Failure(error2) =>
						Success(OptimizedIndexedSeq.from(coll1), failures1 :+ error2)
				}
			case Failure(error1) =>
				other match {
					case Success(coll2, failures2) => Success(OptimizedIndexedSeq.from(coll2), error1 +: failures2)
					case Failure(_) => Failure(error1)
				}
		}
	}
}