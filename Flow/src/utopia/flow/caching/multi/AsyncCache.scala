package utopia.flow.caching.multi

import utopia.flow.caching.single.SingleAsyncCache

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * This cache requests items asynchronously
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
@deprecated("Please consider using some other cache type. This object may be removed or refactored in the future", "v1.10")
object AsyncCache
{
	/**
	  * Creates a new asynchronous cache
	  * @param failResultDuration How long a failed result will be kept
	  * @param requestAsync A function for making asynchronous requests
	  * @param checkResult Checks whether a result should be considered a success
	  * @tparam Key The type of key for this cache
	  * @tparam Value The value provided by this cache
	  * @return A new cache that requests items asynchronously
	  */
	def withCheck[Key, Value](failResultDuration: FiniteDuration)(requestAsync: Key => Future[Value])(checkResult: Value => Boolean) =
		MultiCache[Key, Future[Value], SingleAsyncCache[Value]] {
			key => SingleAsyncCache.withCheck(failResultDuration){ requestAsync(key) }(checkResult) }
	
	/**
	  * Creates a new asynchronous cache
	  * @param failResultDuration How long a failed result will be kept
	  * @param requestAsync A function for making asynchronous requests
	  * @tparam Key The type of key for this cache
	  * @tparam Value The value provided by this cache
	  * @return A new cache that requests items asynchronously
	  */
	def apply[Key, Value](failResultDuration: FiniteDuration)(requestAsync: Key => Future[Value]) =
		withCheck(failResultDuration)(requestAsync) { _ => true }
	
	/**
	  * Creates a new asynchronous cache that wraps results in tries
	  * @param failResultDuration How long a failed result will be kept
	  * @param requestAsync A function for making asynchronous requests
	  * @tparam Key The type of key for this cache
	  * @tparam Value The value provided by this cache
	  * @return A new cache that requests items asynchronously
	  */
	def withTry[Key, Value](failResultDuration: FiniteDuration)(requestAsync: Key => Future[Try[Value]]) =
		withCheck(failResultDuration)(requestAsync) { _.isSuccess }
}
