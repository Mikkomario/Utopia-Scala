package utopia.flow.async

import utopia.flow.collection.immutable.Empty
import utopia.flow.util.TryCatch

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * A utility object used for constructing Futures that contain Tries
  * @author Mikko Hilpinen
  * @since 22.12.2022, v2.0
  */
object TryFuture
{
	// TYPES    ---------------------------
	
	/**
	  * An alias for Future[Try]
	  */
	type Attempt[A] = Future[Try[A]]
	/**
	 * Alias for Future[TryCatch]
	 */
	type CatchingAttempt[A] = Future[TryCatch[A]]
	
	
	// ATTRIBUTES   -----------------------
	
	/**
	  * A successfully completed future without a specific value (i.e. wrapping a Unit)
	  */
	val successCompletion = success(())
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param result A result, which is either success or failure
	  * @tparam A Type of result when successful
	  * @return A new completed future wrapping that result
	  */
	def resolved[A](result: Try[A]) = Future.successful(result)
	/**
	  * @param value A success result
	  * @tparam A Type of that result
	  * @return A completed future that resolved successfully into that value
	  */
	def success[A](value: A) = Future.successful(Success(value))
	/**
	 * @param value Success result
	 * @param errors Caught failures (optional)
	 * @tparam A Type of the successfully acquired value
	 * @return A new resolved future that contains a successful TryCatch
	 */
	def successCatching[A](value: A, errors: IndexedSeq[Throwable] = Empty) =
		Future.successful(TryCatch.Success(value, errors))
	/**
	  * @param error An error / cause of failure
	  * @tparam A Type of success value
	  * @return A completed future that failed with that cause / error
	  */
	def failure[A](error: Throwable) = Future.successful(Failure[A](error))
}
