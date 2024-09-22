package utopia.flow.util

import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger

import scala.collection.immutable.VectorBuilder
import scala.util.{Failure, Success, Try}

/**
  * Introduces additional functions related to [[Try]] and [[TryCatch]]
  * @author Mikko Hilpinen
  * @since 22.09.2024, v2.5
  */
object TryExtensions
{
	implicit class RichTry[A](val t: Try[A]) extends AnyVal
	{
		/**
		  * The success value of this try. None if this try was a failure
		  */
		def success = t.toOption
		/**
		  * The failure (throwable) value of this try. None if this try was a success.
		  */
		def failure = t.failed.toOption
		
		/**
		  * @return A TryCatch instance based on this Try
		  */
		def toTryCatch: TryCatch[A] = t match {
			case Success(a) => TryCatch.Success(a)
			case Failure(e) => TryCatch.Failure(e)
		}
		
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
		  * @param log Logging implementation to use
		  */
		@deprecated("Deprecated for removal. Please use .log instead", "v2.5")
		def logFailure(implicit log: Logger) = failure.foreach { log(_) }
		/**
		  * Logs the captured failure, if applicable
		  * @param message Message to record with the failure (call-by-name)
		  * @param log Logging implementation to use
		  */
		@deprecated("Deprecated for removal. Please use .logWithMessage instead", "v2.5")
		def logFailureWithMessage(message: => String)(implicit log: Logger) = failure.foreach { log(_, message) }
		
		/**
		  * Converts this try into an option. Logs possible failure state.
		  * @param log Implicit logger to use to log the potential failure.
		  * @return Some if success, None otherwise
		  */
		@deprecated("Renamed to .log", "v2.5")
		def logToOption(implicit log: Logger) = this.log
		/**
		  * Converts this try into an option. Logs possible failure state.
		  * @param message Message to log in case of a failure (call-by-name)
		  * @param log Implicit logger to use to log the potential failure.
		  * @return Some if success, None otherwise
		  */
		@deprecated("Renamed to .logWithMessage(...)", "v2.5")
		def logToOptionWithMessage(message: => String)(implicit log: Logger) = logWithMessage(message)
		
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
		def handleFailure[U](f: Throwable => U) = t match {
			case Success(v) => Some(v)
			case Failure(error) => f(error); None
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
	}
	
	implicit class RichTryTryCatch[A](val t: Try[TryCatch[A]]) extends AnyVal
	{
		/**
		  * @return Flattened copy of this 2-level try into a single TryCatch instance
		  */
		def flattenCatching: TryCatch[A] = t.getOrMap { TryCatch.Failure(_) }
	}
	
	implicit class TriesIterableOnce[A](val tries: IterableOnce[Try[A]]) extends AnyVal
	{
		/**
		  * Converts this series of attempts to a single try. The resulting try succeeds only if all attempts succeeded.
		  * If a failure is encountered, iteration is immediately ended and that failure is returned.
		  * @return Success containing all success results or Failure containing the encountered error
		  */
		def toTry = {
			val successesBuilder = new VectorBuilder[A]()
			val iter = tries.iterator
			var failure: Option[Throwable] = None
			while (failure.isEmpty && iter.hasNext) {
				iter.next() match {
					case Success(item) => successesBuilder += item
					case Failure(error) => failure = Some(error)
				}
			}
			failure match {
				case Some(error) => Failure(error)
				case None => Success(successesBuilder.result())
			}
		}
		/**
		  * @return Failure if all attempts in this collection failed, containing the first encountered error.
		  *         If one or more attempts succeeded, or if no attempts were made, returns a success containing
		  *         caught errors, as well as successes
		  */
		def toTryCatch: TryCatch[Vector[A]] = {
			val (failures, successes) = tries.divided
			if (successes.isEmpty) {
				failures.headOption match {
					// Case: All attempts failed => fails with the first encountered error
					case Some(firstError) => TryCatch.Failure(firstError)
					// Case: No attempts were made => empty success
					case None => TryCatch.Success(successes, failures)
				}
			}
			// Case: One or more attempts succeeded => success
			else
				TryCatch.Success(successes, failures)
		}
		
		/**
		  * Divides this collection to two separate collections, one for failures and one for successes
		  * @return Failures + successes
		  */
		def divided = {
			val successesBuilder = new VectorBuilder[A]
			val failuresBuilder = new VectorBuilder[Throwable]
			tries.iterator.foreach {
				case Success(a) => successesBuilder += a
				case Failure(error) => failuresBuilder += error
			}
			failuresBuilder.result() -> successesBuilder.result()
		}
		
		/**
		  * @return An iterator that only includes failed attempts
		  */
		def failuresIterator = tries.iterator.flatMap { _.failure }
		/**
		  * @return The first failure that was encountered. None if no failures were encountered.
		  */
		def anyFailure = tries.iterator.findMap { _.failure }
	}
	
	implicit class TryCatchesIterableOnce[A](val tries: IterableOnce[TryCatch[A]]) extends AnyVal
	{
		/**
		  * @return Success if at least one of the items in this collection was a success,
		  *         or if this collection is empty.
		  *         Failure otherwise.
		  */
		def toTryCatch: TryCatch[IndexedSeq[A]] = {
			// Collects all success and failure values, including partial failures
			val successesBuilder = new VectorBuilder[A]()
			val failuresBuilder = new VectorBuilder[Throwable]()
			tries.iterator.foreach {
				case TryCatch.Success(v, failures) =>
					successesBuilder += v
					failuresBuilder ++= failures
				case TryCatch.Failure(error) => failuresBuilder += error
			}
			val failures = failuresBuilder.result()
			successesBuilder.result().notEmpty match {
				// Case: There was at least one success => Succeeds
				case Some(successes) => TryCatch.Success(successes, failures)
				case None =>
					failures.headOption match {
						// Case: No successes => Fails
						case Some(error) => TryCatch.Failure(error)
						// Case: Empty collection => Succeeds
						case None => TryCatch.Success(Empty)
					}
			}
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
