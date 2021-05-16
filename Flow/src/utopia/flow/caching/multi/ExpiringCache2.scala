package utopia.flow.caching.multi

import utopia.flow.async.Volatile
import utopia.flow.collection.VolatileList
import utopia.flow.time.{Now, WaitUtils}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.CollectionExtensions._

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object ExpiringCache2
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
	def apply[K, V](request: K => V)(calculateExpiration: (K, V) => Duration)
	               (implicit exc: ExecutionContext) = new ExpiringCache2[K, V](request)(calculateExpiration)
	
	/**
	  * Creates a new expiring cache
	  * @param expirationDuration A duration after which generated values expire
	  * @param request Function for requesting new values
	  * @param exc Implicit execution context
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new cache that clears values automatically
	  */
	def apply[K, V](expirationDuration: FiniteDuration)(request: K => V)(implicit exc: ExecutionContext) =
		new ExpiringCache2[K, V](request)((_, _) => expirationDuration)
}

/**
  * A cache which removes its contents a while after they have been generated
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class ExpiringCache2[K, V](request: K => V)(calculateExpiration: (K, V) => Duration)
                          (implicit exc: ExecutionContext) extends CacheLike[K, V]
{
	// ATTRIBUTES   -----------------------------
	
	private val waitLock = new AnyRef
	
	private val cachePointer = Volatile(Map[K, V]())
	
	private val queuedExpirationsPointer = VolatileList[(Instant, K)]()
	private var expirationFuture = Future.successful(())
	
	
	// IMPLEMENTED  -----------------------------
	
	override def apply(key: K) = cached(key).getOrElse {
		// Generates and stores the new value
		val newValue = request(key)
		cachePointer.update { _ + (key -> newValue) }
		// Prepares an expiration if necessary
		calculateExpiration(key, newValue).finite.foreach { expirationDuration =>
			// Queues a new expiration
			val expirationTime = Now + expirationDuration
			val (queueWasEmpty, needsNotify) = queuedExpirationsPointer.pop { queue =>
				if (queue.isEmpty)
					(true, false) -> Vector(expirationTime -> key)
				else
				{
					val (needsNotify, newQueue) = queue.lastIndexWhereOption { case (time, _) => time < expirationTime } match
					{
						case Some(previousIndex) =>
							val newQueue = (queue.take(previousIndex + 1) :+
								(expirationTime -> key)) ++ queue.drop(previousIndex + 1)
							false -> newQueue
						case None =>
							val newQueue = (expirationTime -> key) +: queue
							true -> newQueue
					}
					(false -> needsNotify) -> newQueue
				}
			}
			// Starts asynchronous expiration process if necessary
			if (queueWasEmpty)
				expirationFuture = Future {
					// Handles expirations as long as there are some available
					Iterator.continually { queuedExpirationsPointer.value.headOption }
						.takeWhile { _.isDefined }.flatten
						.foreach { case (waitTarget, targetKey) =>
							// Waits until the wait target is reached (may be interrupted)
							WaitUtils.waitUntil(waitTarget, waitLock)
							// If target was reached, removes the key from the cache
							// Otherwise finds a new target
							if (Now >= waitTarget)
							{
								cachePointer.update { _ - targetKey }
								queuedExpirationsPointer.update { _.filterNot { _._2 == targetKey } }
							}
						}
				}
			else if (needsNotify)
				WaitUtils.notify(waitLock)
		}
		// Finally returns the newly calculated value
		newValue
	}
	
	override def cached(key: K) = cachePointer.value.get(key)
}
