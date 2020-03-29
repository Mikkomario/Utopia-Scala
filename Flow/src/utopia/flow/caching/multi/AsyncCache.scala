package utopia.flow.caching.multi

import utopia.flow.caching.single.{ExpiringSingleCache, SingleAsyncCache}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * This cache requests items asynchronously
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
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
	
	/**
	  * Creates a new asynchronous cache that expires both success and failure results
	  * @param failResultDuration How long a failed result is cached
	  * @param maxResultDuration How long a success result is cached (should be larger than failResultDuration)
	  * @param requestAsync A function for performing asynchronous requests
	  * @param checkResult A function for checking whether a result should be considered a success or a failure
	  * @tparam Key The type of key for this cache
	  * @tparam Value The type of values asynchronously returned through this cache
	  * @return A cache that requests items asynchronously and also expires them
	  */
	def expiringWithCheck[Key, Value](failResultDuration: FiniteDuration, maxResultDuration: FiniteDuration)(
		requestAsync: Key => Future[Value])(checkResult: Value => Boolean) = ExpiringCache[Key, Future[Value]] {
		key: Key => ExpiringSingleCache.wrap(SingleAsyncCache.withCheck(failResultDuration){ requestAsync(key) }(checkResult), maxResultDuration) }
	
	/**
	  * Creates a new asynchronous cache that expires both success and failure results
	  * @param failResultDuration How long a failed result is cached
	  * @param maxResultDuration How long a success result is cached (should be larger than failResultDuration)
	  * @param requestAsync A function for performing asynchronous requests
	  * @tparam Key The type of key for this cache
	  * @tparam Value The type of values asynchronously returned through this cache
	  * @return A cache that requests items asynchronously and also expires them
	  */
	def expiring[Key, Value](failResultDuration: FiniteDuration, maxResultDuration: FiniteDuration)(
		requestAsync: Key => Future[Value]) =
		expiringWithCheck(failResultDuration, maxResultDuration)(requestAsync) { _ => true }
	
	/**
	  * Creates a new asynchronous cache that expires both success and failure results
	  * @param failResultDuration How long a failed result is cached
	  * @param maxResultDuration How long a success result is cached (should be larger than failResultDuration)
	  * @param requestAsync A function for performing asynchronous requests
	  * @tparam Key The type of key for this cache
	  * @tparam Value The type of values asynchronously returned through this cache
	  * @return A cache that requests items asynchronously and also expires them
	  */
	def expiringTry[Key, Value](failResultDuration: FiniteDuration, maxResultDuration: FiniteDuration)(
		requestAsync: Key => Future[Try[Value]]) =
		expiringWithCheck(failResultDuration, maxResultDuration)(requestAsync) { _.isSuccess }
}
