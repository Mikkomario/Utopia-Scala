package utopia.flow.async

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
	  * @param error An error / cause of failure
	  * @tparam A Type of success value
	  * @return A completed future that failed with that cause / error
	  */
	def failure[A](error: Throwable) = Future.successful(Failure[A](error))
}
