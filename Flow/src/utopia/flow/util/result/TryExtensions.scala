package utopia.flow.util.result

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.util.Mutate
import utopia.flow.util.logging.Logger

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
  * Introduces additional functions related to [[Try]] and [[TryCatch]]
  * @author Mikko Hilpinen
  * @since 22.09.2024, v2.5
  */
object TryExtensions
{
	implicit class RichTry[+A](val t: Try[A])
		extends AnyVal with MayHaveFailed[A] with MayHaveFailedLike[A, RichTry, RichTry, TryCatch]
	{
		// COMPUTED --------------------------
		
		/**
		 * Converts this try into an option. Logs possible failure state.
		 * @param log Implicit logger to use to log the potential failure.
		 * @return Some if success, None otherwise
		 */
		def log(implicit log: Logger) = t match {
			case Success(a) => Some(a)
			case Failure(error) =>
				log(error)
				None
		}
		/**
		 * Converts this try into an option. Logs possible failure state.
		 * @param log Implicit logger to use to log the potential failure.
		 * @return Some if success, None otherwise
		 */
		@deprecated("Renamed to .log", "v2.5")
		def logToOption(implicit log: Logger) = this.log
		
		/**
		 * Logs the captured failure, if applicable
		 * @param log Logging implementation to use
		 */
		@deprecated("Deprecated for removal. Please use .log instead", "v2.5")
		def logFailure(implicit log: Logger) = failure.foreach { log(_) }
		
		
		// IMPLEMENTED  ----------------------
		
		override def isSuccess: Boolean = t.isSuccess
		override def isFailure: Boolean = t.isFailure
		
		override def success = t.toOption
		override def failure = t match {
			case Failure(error) => Some(error)
			case _ => None
		}
		override def get: A = t.get
		
		override def toTry: Try[A] = t
		override def toTryCatch: TryCatch[A] = t match {
			case Success(a) => TryCatch.Success(a)
			case Failure(e) => TryCatch.Failure(e)
		}
		
		override def catching[B >: A](partialFailures: => IterableOnce[Throwable]): TryCatch[B] = t match {
			case Success(v) => TryCatch.Success(v, OptimizedIndexedSeq.from(partialFailures))
			case Failure(error) => TryCatch.Failure(error)
		}
		
		override def map[B](f: A => B): RichTry[B] = t.map(f)
		override def tryMap[B](f: A => Try[B]): RichTry[B] = t.flatMap(f)
		override def mapOrFail[B](f: A => MayHaveFailed[B]): RichTry[B] = t match {
			case Success(value) => f(value).toTry
			case Failure(error) => Failure(error)
		}
		override def tryMapCatching[B](f: A => TryCatch[B]): TryCatch[B] = flatMapCatching(f)
		
		
		// OTHER    --------------------------
		
		/**
		 * @param f A mapping function for possible failure
		 * @tparam B Result type
		 * @return Contents of this try on success, mapped error on failure
		 */
		def getOrMap[B >: A](f: Throwable => B): B = t match {
			case Success(item) => item
			case Failure(error) => f(error)
		}
		
		/**
		 * @param f A mapping function applied if this is a failure
		 * @return If this is a success, returns self. Otherwise, returns a mapped failure.
		 */
		def mapFailure(f: Mutate[Throwable]) = t match {
			case s: Success[A] => s
			case Failure(error) => Failure(f(error))
		}
		
		/**
		 * @param f A function called on a failure
		 * @tparam U Arbitrary 'f' result type
		 * @return Some if this was a success, None if failure.
		 */
		def forFailure[U](f: Throwable => U) = t match {
			case Success(v) => Some(v)
			case Failure(error) =>
				f(error)
				None
		}
		
		/**
		 * Maps the value of this Try, if successful.
		 * @param f A mapping function that accepts a successfully acquired value and returns a
		 *          TryCatch instance.
		 * @tparam B Type of the success value in the map function result
		 * @return Success containing the mapping result and the possible non-critical failures,
		 *         or a failure.
		 */
		def flatMapCatching[B](f: A => TryCatch[B]) = t match {
			case Success(v) => f(v)
			case Failure(e) => TryCatch.Failure(e)
		}
		
		/**
		 * @param error A (secondary) error (call-by-name)
		 * @tparam B Type of failure to yield
		 * @return This if failure, otherwise a failure based on the specified error
		 */
		def failWith[B](error: => Throwable) = t match {
			case Success(_) => Failure[B](error)
			case Failure(e) => Failure[B](e)
		}
		/**
		 * @param error A potential error (call-by-name, not called if this is a failure already)
		 * @return Success only if this is a success and the specified error is None.
		 *         Failure otherwise, preferring an existing failure in this Try, if applicable.
		 */
		def failIf(error: => Option[Throwable]) = {
			t match {
				case Success(v) =>
					error match {
						// Case: This was a success but the specified function failed => Fails
						case Some(e) => Failure(e)
						// Case: This was a success and the specified function didn't fail => Success
						case None => Success(v)
					}
				// Case: This was already a failure => Fails
				case Failure(e) => Failure(e)
			}
		}
		
		/**
		  * Converts this try into an option. Logs possible failure state.
		  * @param message Message to log in case of a failure (call-by-name)
		  * @param log Implicit logger to use to log the potential failure.
		  * @return Some if success, None otherwise
		  */
		def logWithMessage(message: => String)(implicit log: Logger) = t match {
			case Success(a) => Some(a)
			case Failure(error) =>
				log(error, message)
				None
		}
		/**
		  * Logs the captured failure, if applicable
		  * @param message Message to record with the failure (call-by-name)
		  * @param log Logging implementation to use
		  */
		@deprecated("Deprecated for removal. Please use .logWithMessage instead", "v2.5")
		def logFailureWithMessage(message: => String)(implicit log: Logger) = failure.foreach { log(_, message) }
		/**
		  * Converts this try into an option. Logs possible failure state.
		  * @param message Message to log in case of a failure (call-by-name)
		  * @param log Implicit logger to use to log the potential failure.
		  * @return Some if success, None otherwise
		  */
		@deprecated("Renamed to .logWithMessage(...)", "v2.5")
		def logToOptionWithMessage(message: => String)(implicit log: Logger) = logWithMessage(message)
		
		/**
		  * Returns the success value or logs the error and returns a placeholder value
		  * @param f A function for generating the returned value in case of a failure
		  * @param log Implicit logging implementation for encountered errors
		  * @tparam B Result type
		  * @return Successful contents of this try, or the specified placeholder value
		  */
		@deprecated("Deprecated for removal. Please use .log.getOrElse(...) instead", "v2.5")
		def getOrElseLog[B >: A](f: => B)(implicit log: Logger): B = getOrMap { error =>
			log(error)
			f
		}
		/**
		  * @param f A function called if this is a success
		  * @param log Implicit logger to record a possible failure with
		  * @tparam U Arbitrary function result type
		  */
		@deprecated("Deprecated for removal. Please use .log.foreach(...) instead", "v2.5")
		def foreachOrLog[U](f: A => U)(implicit log: Logger): Unit = t match {
			case Success(a) => f(a)
			case Failure(error) => log(error)
		}
		
		/**
		  * Converts this Try into an Option.
		  * Handles the possible failure case using the specified function.
		  * @param f A function that handles the possible failure case
		  * @tparam U Arbitrary function result type
		  * @return Some if this was a success, None of failure.
		  */
		@deprecated("Renamed to .forFailure(...)", "v2.8")
		def handleFailure[U](f: Throwable => U) = forFailure(f)
	}
	
	implicit class TriesIterableOnce[A](val tries: IterableOnce[Try[A]]) extends AnyVal with Attempts[A, Try[A], Try]
	{
		// COMPUTED -------------------------
		
		/**
		  * Converts this series of attempts to a single try. The resulting try succeeds only if all attempts succeeded.
		  * If a failure is encountered, iteration is immediately ended and that failure is returned.
		  * @return Success containing all success results or Failure containing the encountered error
		  */
		def toTry = tryFlatten[A]
		
		/**
		  * @return The first failure that was encountered. None if no failures were encountered.
		  */
		@deprecated("Renamed to .firstFailure", "v2.8")
		def anyFailure = tries.iterator.findMap { _.failure }
		
		
		// IMPLEMENTED  -----------------------
		
		override protected def iterator: Iterator[Try[A]] = tries.iterator
		
		override def divided = tries.divideWith {
			case Success(v) => Right(v)
			case Failure(error) => Left(error)
		}
		
		override protected def wrap(result: Try[A]): MayHaveFailed[A] = result
		override protected def unwrap[B](result: MayHaveFailed[B]): Try[B] = result.toTry
		
		override def toTryCatchUsing[To <: Iterable[_]](builder: mutable.Builder[A, To]) = {
			val resultBuilder = TryCatch.builder.wrap(builder)
			resultBuilder ++= tries
			resultBuilder.result()
		}
	}
	
	implicit class TryCatchesIterableOnce[A](val tries: IterableOnce[TryCatch[A]])
		extends AnyVal with Attempts[A, TryCatch[A], TryCatch]
	{
		override protected def iterator: Iterator[TryCatch[A]] = tries.iterator
		
		override protected def wrap(result: TryCatch[A]): MayHaveFailed[A] = result
		override protected def unwrap[B](result: MayHaveFailed[B]): TryCatch[B] = result.toTryCatch
		
		override def toTryCatchUsing[To <: Iterable[_]](builder: mutable.Builder[A, To]): TryCatch[To] = {
			val resultBuilder = TryCatch.builder.wrap(builder)
			resultBuilder.catching ++= tries
			resultBuilder.result()
		}
	}
	
	implicit class TryIterator[A](val i: Iterator[Try[A]]) extends AnyVal
	{
		/**
		  * Iterates until the first success is encountered.
		  * If this iterator doesn't contain a single success, iterates over all items.
		  * @return The first success that was encountered,
		  *         including any failures that were encountered before that success.
		  *         If no successes were encountered, returns the last encountered failure.
		  *         Also fails if this iterator is empty
		  */
		def trySucceedOnce: TryCatch[A] = {
			val results = i.iterator.collectTo { _.isSuccess }
			results.lastOption match {
				case Some(lastResult) =>
					lastResult match {
						case Success(a) => TryCatch.Success(a, results.flatMap { _.failure })
						case Failure(error) => TryCatch.Failure(error)
					}
				case None => TryCatch.Failure(new IllegalStateException("trySucceedOnce called for an empty iterator"))
			}
		}
		
		/**
		  * @param f A mapping function performed for successful elements
		  * @tparam B Mapping result type
		  * @return Copy of this iterator where all successful elements are mapped (lazily)
		  */
		def mapSuccesses[B](f: A => B): Iterator[Try[B]] = i.map { _.map(f) }
		/**
		  * @param f A mapping function that may yield a failure
		  * @tparam B Type of mapping results, when successful
		  * @return Copy of this iterator where successful results are mapped using the specified function.
		  *         The mapping is performed on-call only.
		  */
		def flatMapSuccesses[B](f: A => Try[B]): Iterator[Try[B]] = i.map {
			case Success(item) => f(item)
			case Failure(error) => Failure(error)
		}
	}
}
