package utopia.flow.collection.immutable.caching.cache

import utopia.flow.async.process.WaitTarget.Until
import utopia.flow.async.process.{LoopingProcess, Wait, WaitUtils}
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile

import java.time.Instant
import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.ExecutionContext

object ExpiringUnusedCache
{
	/**
	 * Creates a new expiring cache
	 * @param request Function for requesting new values
	 * @param calculateExpiration Function for calculating an expiration threshold for a key-value pair
	 * @param exc Implicit execution context
	 * @tparam K Type of keys used
	 * @tparam V Type of values stored
	 * @return A new cache that clears values automatically
	 */
	def apply[K, V](request: K => V)(calculateExpiration: (K, V) => Option[Instant])
	               (implicit exc: ExecutionContext, log: Logger) =
		new ExpiringUnusedCache[K, V](request)(calculateExpiration)
	
	/**
	 * Creates a new expiring cache
	 * @param expirationDuration A duration after which generated values expire
	 * @param request Function for requesting new values
	 * @param exc Implicit execution context
	 * @tparam K Type of keys used
	 * @tparam V Type of values stored
	 * @return A new cache that clears values automatically
	 */
	def after[K, V](expirationDuration: Duration)(request: K => V)(implicit exc: ExecutionContext, log: Logger) =
		new ExpiringUnusedCache[K, V](request)((_, _) => Some(Now + expirationDuration))
}

/**
  * A cache which removes its contents a while after they have been generated
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class ExpiringUnusedCache[-K, +V](request: K => V)(calculateExpiration: (K, V) => Option[Instant])
                                 (implicit exc: ExecutionContext, log: Logger)
	extends Cache[K, V]
{
	// ATTRIBUTES   -----------------------------
	
	private val waitLock = new AnyRef
	// Unchecked, because all values are generated using 'request'
	private val cacheP: Volatile[Map[Any, (V @uncheckedVariance, Volatile[Option[Instant]])]] = Volatile(Map())
	private val nextExpirationP = Volatile.empty[Instant]
	
	
	// IMPLEMENTED  -----------------------------
	
	override def cachedValues = cacheP.value.values.view.map { _._1 }
	
	override def apply(key: K) = {
		// Acquires the cached value or caches a new value
		val (value, expiresView) = cacheP.mutate { cache =>
			cache.get(key) match {
				case Some(value) => value -> cache
				case None =>
					val value = request(key) -> Volatile.empty[Instant]
					value -> (cache + (key -> value))
			}
		}
		// Sets the new expiration time
		val expires = calculateExpiration(key, value)
		expiresView.value = expires
		
		// Starts or hurries the expiration process, if necessary
		expires.foreach { expires =>
			val (shouldStart, shouldNotify) = nextExpirationP.mutate {
				case Some(nextTarget) =>
					// Case: Next expiration is sooner than what before => Hurries
					if (nextTarget > expires)
						(false, true) -> Some(expires)
					// Case: Next expiration was not modified => No change
					else
						(false, false) -> Some(nextTarget)
				
				// Case: No expiration was set previously => Starts
				case None => (true, false) -> Some(expires)
			}
			if (shouldStart)
				ExpirationProcess.runAsync()
			else if (shouldNotify)
				WaitUtils.notify(waitLock)
		}
		
		value
	}
	override def cached(key: K) = cacheP.value.get(key).map { _._1 }
	
	
	// OTHER    ---------------------------
	
	// Locks the next expiration pointer from modifications during this clearance
	private def clearExpired() = nextExpirationP.updateAndGet { _ =>
		// Removes expired values from the cache
		cacheP.mutate { cached =>
			val now = Now.toInstant
			var nextTarget: Option[Instant] = None
			val remaining = cached.filter { case (_, (_, expirationView)) =>
				expirationView.value.forall { expires =>
					// Case: Expired => Removes
					if (expires <= now)
						false
					// Case: Not expired => Checks whether that becomes the next target
					else {
						if (nextTarget.forall { _ > expires })
							nextTarget = Some(expires)
						true
					}
				}
			}
			nextTarget -> remaining
		}
	}
	
	
	// NESTED   ---------------------------
	
	private object ExpirationProcess extends LoopingProcess(waitLock = waitLock)
	{
		// ATTRIBUTES   -------------------
		
		override protected val isRestartable = true
		
		
		// IMPLEMENTED  -------------------
		
		override protected def iteration() = {
			// Checks the next expiration
			nextExpirationP.value match {
				case Some(target) =>
					// Waits until the wait target is reached, clears the expired elements
					// and schedules the next iteration
					if (Wait(target, this.waitLock))
						clearExpired().map { Until(_) }
					// Case: Interrupted => Stops
					else {
						markAsInterrupted()
						None
					}
					
				// Case: Nothing to expire => Completes
				case None => None
			}
		}
	}
}
