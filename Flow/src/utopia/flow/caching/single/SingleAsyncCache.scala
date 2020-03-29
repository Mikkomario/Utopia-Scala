package utopia.flow.caching.single

import java.time.Instant

import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.TimeExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object SingleAsyncCache
{
	/**
	  * Creates a new single asynchronous cache
	  * @param failCacheDuration The duration which a failed result will be cached
	  * @param makeRequest A function for making a new request
	  * @param checkResult Checks whether the result should be considered a success
	  * @tparam A The type of returned item
	  * @return A new cache that requests items asynchronously
	  */
	def withCheck[A](failCacheDuration: FiniteDuration)(makeRequest: => Future[A])(checkResult: A => Boolean): SingleAsyncCache[A] =
		new SingleAsyncCacheImpl(failCacheDuration, () => makeRequest, checkResult)
	
	/**
	  * Creates a new single asynchronous cache
	  * @param failCacheDuration The duration which a failed result will be cached
	  * @param makeRequest A function for making a new request
	  * @tparam A The type of returned item
	  * @return A new cache that requests items asynchronously
	  */
	def apply[A](failCacheDuration: FiniteDuration)(makeRequest: => Future[A]) =
		withCheck(failCacheDuration)(makeRequest) { _ => true }
	
	/**
	  * Creates a new single asynchronous cache. Results are wrapped in Try
	  * @param failCacheDuration The duration which a failed result will be cached
	  * @param makeRequest A function for making a new request
	  * @tparam A The type of returned item
	  * @return A new cache that requests items asynchronously
	  */
	def withTry[A](failCacheDuration: FiniteDuration)(makeRequest: => Future[Try[A]]): SingleAsyncCache[Try[A]] =
		withCheck(failCacheDuration)(makeRequest) { _.isSuccess }
	
	/**
	  * Creates a new single asynchronous cache. Results are wrapped in Try.
	  * @param failCacheDuration The duration which a failed result will be cached
	  * @param getResult A function for making a new request
	  * @param context Implicit execution context
	  * @tparam A Type of returned item on success
	  * @return A new cache
	  */
	def tryAsync[A](failCacheDuration: FiniteDuration)(getResult: => Try[A])(implicit context: ExecutionContext) =
		withTry(failCacheDuration) { Future(getResult) }
}

/**
  * This cache caches asynchronous requests
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
trait SingleAsyncCache[A] extends ClearableSingleCacheLike[Future[A]]
{
	// ATTRIBUTES	--------------
	
	private var lastRequestTime: Option[Instant] = None
	private var lastRequest: Option[Future[A]] = None
	
	
	// ABSTRACT	------------------
	
	/**
	  * Requests a new value
	  * @return The asynchronous request results
	  */
	protected def request(): Future[A]
	
	/**
	  * Checks whether an item represents a success
	  * @param item An item
	  * @return Whether the item should be considered a success result
	  */
	protected def isSuccess(item: A): Boolean
	
	/**
	  * The duration how long a failed request will be cached
	  */
	protected val failCacheDuration: FiniteDuration
	
	
	// IMPLEMENTED	--------------
	
	// Cached request doesn't count if it has already failed and failure duration has passed since the request was made
	override def cached = lastRequest.filter { r => !r.isCompleted ||
		r.waitFor().toOption.exists(isSuccess) || lastRequestTime.exists { Instant.now() < _ + failCacheDuration } }
	
	override def apply() =
	{
		cached.getOrElse
		{
			val result = request()
			lastRequestTime = Some(Instant.now())
			lastRequest = Some(result)
			
			result
		}
	}
	
	override def clear() =
	{
		lastRequest = None
		lastRequestTime = None
	}
}

private class SingleAsyncCacheImpl[A](protected val failCacheDuration: FiniteDuration,
									  private val makeRequest: () => Future[A], private val checkSuccess: A => Boolean)
	extends SingleAsyncCache[A]
{
	override protected def isSuccess(item: A) = checkSuccess(item)
	
	override protected def request() = makeRequest()
}
