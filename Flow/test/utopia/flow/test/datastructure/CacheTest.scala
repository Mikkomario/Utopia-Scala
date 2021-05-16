package utopia.flow.test.datastructure

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ThreadPool
import utopia.flow.caching.multi._
import utopia.flow.caching.single.{ClearableSingleCache, ExpiringSingleCache, SingleAsyncCache, SingleTryCache}
import utopia.flow.datastructure.mutable.Pointer
import utopia.flow.time.WaitUtils
import utopia.flow.time.TimeExtensions._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * This test tests various cache implementations
 * @author Mikko Hilpinen
 * @since 12.6.2019, v1.5+
 */
object CacheTest extends App
{
	// Clearable single cache
	val clearableSingle = ClearableSingleCache(Instant.now())
	
	assert(!clearableSingle.isValueCached)
	assert(clearableSingle.cached.isEmpty)
	
	val result1 = clearableSingle()
	
	assert(clearableSingle.isValueCached)
	assert(clearableSingle.cached.contains(result1))
	
	clearableSingle.clear()
	
	assert(!clearableSingle.isValueCached)
	assert(clearableSingle.cached.isEmpty)
	
	// Expiring single cache
	val expireTime = 0.25.seconds
	val waitLock = new AnyRef()
	
	val expiringSingle = ExpiringSingleCache(expireTime)(Instant.now())
	
	assert(!expiringSingle.isValueCached)
	assert(expiringSingle.cached.isEmpty)
	
	val result2 = expiringSingle()
	
	assert(expiringSingle.isValueCached)
	assert(!expiringSingle.isDataExpired)
	
	WaitUtils.wait(0.5.seconds, waitLock)
	
	assert(expiringSingle.isDataExpired)
	assert(!expiringSingle.isValueCached)
	assert(expiringSingle() != result2)
	
	// Single try cache
	val failureTime = 0.15.seconds
	
	val failSingle = SingleTryCache(failureTime) { Failure(new NullPointerException()) }
	val successSingle = SingleTryCache(failureTime) { Success(Instant.now()) }
	
	assert(!failSingle.isValueCached)
	assert(!failSingle.isFailureCached)
	assert(!successSingle.isValueCached)
	assert(!successSingle.isFailureCached)
	
	assert(failSingle().isFailure)
	val result3 = successSingle()
	assert(result3.isSuccess)
	
	assert(failSingle.isValueCached)
	assert(failSingle.isFailureCached)
	assert(successSingle.isValueCached)
	assert(successSingle.isSuccessCached)
	
	WaitUtils.wait(0.25.seconds, waitLock)
	
	assert(!failSingle.isValueCached)
	assert(successSingle.isValueCached)
	
	successSingle.clear()
	
	assert(!successSingle.isValueCached)
	
	assert(successSingle() != result3)
	
	// Single async cache
	implicit val asyncContext: ExecutionContext = new ThreadPool("CacheTest").executionContext
	val asyncRequestTime = 0.05.seconds
	
	val singleAsyncFail = SingleAsyncCache(failureTime) { Future { WaitUtils.wait(asyncRequestTime, new AnyRef); throw new NullPointerException } }
	val singleAsyncSuccess = SingleAsyncCache(failureTime) { Future { WaitUtils.wait(asyncRequestTime, new AnyRef); Instant.now() } }
	
	assert(!singleAsyncFail.isValueCached)
	assert(!singleAsyncSuccess.isValueCached)
	
	val asyncFailResult = singleAsyncFail()
	val asyncSuccessResult = singleAsyncSuccess()
	
	assert(!asyncFailResult.isCompleted)
	assert(!asyncSuccessResult.isCompleted)
	
	assert(asyncFailResult.waitFor().isFailure)
	assert(asyncSuccessResult.waitFor().isSuccess)
	
	assert(singleAsyncFail.isValueCached)
	assert(singleAsyncSuccess.isValueCached)
	
	WaitUtils.wait(failureTime, waitLock)
	
	assert(!singleAsyncFail.isValueCached)
	assert(singleAsyncSuccess.isValueCached)
	
	val tryFuture = SingleAsyncCache.tryAsync(failureTime) { WaitUtils.wait(asyncRequestTime, new AnyRef); Failure(new NullPointerException) }
	
	assert(!tryFuture.isValueCached)
	assert(tryFuture().waitForResult().isFailure)
	assert(tryFuture.isValueCached)
	
	WaitUtils.wait(failureTime, waitLock)
	
	assert(!tryFuture.isValueCached)
	
	// Cache
	private var cacheRequests = 0
	val cache = Cache[Int, Int] { i => cacheRequests += 1; i + cacheRequests }
	
	assert(!cache.isValueCached(0))
	
	val result4 = cache(0)
	cache(1)
	
	assert(cache.isValueCached(0))
	assert(cache.isValueCached(1))
	assert(!cache.isValueCached(2))
	assert(cache(0) == result4)
	assert(cacheRequests == 2)
	
	// TryCache
	cacheRequests = 0
	val tryCache = TryCache[Int, Int](failureTime) { i =>
		cacheRequests += 1
		if (i < 0) Failure(new NullPointerException()) else Success(i)
	}
	
	assert(tryCache(2).isSuccess)
	assert(tryCache(-2).isFailure)
	assert(tryCache.isValueCached(2))
	assert(tryCache.isValueCached(-2))
	assert(cacheRequests == 2)
	
	WaitUtils.wait(0.5.seconds, waitLock)
	
	assert(tryCache.isValueCached(2))
	assert(!tryCache.isValueCached(-2))
	assert(tryCache(2).isSuccess)
	assert(tryCache(-2).isFailure)
	assert(cacheRequests == 3)
	
	// Async cache
	cacheRequests = 0
	val asyncCache = AsyncCache.withTry[Int, Int](failureTime) { i =>
		Future {
			cacheRequests += 1
			WaitUtils.wait(asyncRequestTime, new AnyRef)
			if (i < 0) Failure(new NullPointerException) else Success(i)
		}
	}
	
	val result6 = asyncCache(3)
	val result7 = asyncCache(-3)
	
	assert(asyncCache.isValueCached(3))
	assert(asyncCache.isValueCached(-3))
	assert(result6.waitForResult().isSuccess)
	assert(result7.waitForResult().isFailure)
	assert(cacheRequests == 2)
	
	WaitUtils.wait(failureTime, waitLock)
	
	assert(asyncCache.isValueCached(3))
	assert(!asyncCache.isValueCached(-3))
	assert(asyncCache(3).isCompleted)
	assert(!asyncCache(-3).isCompleted)
	assert(asyncCache.isValueCached(-3))
	
	WaitUtils.wait(asyncRequestTime, waitLock)
	
	assert(asyncCache.isValueCached(3))
	assert(asyncCache.isValueCached(-3))
	assert(asyncCache(-3).isCompleted)
	assert(cacheRequests == 3)
	
	println("Success!")
}
