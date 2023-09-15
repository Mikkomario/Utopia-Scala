package utopia.flow.collection.immutable.caching.cache

import utopia.flow.async.process.{LoopingProcess, Wait, WaitUtils}
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.collection.template.CacheLike
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.Now
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.Volatile

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

object ExpiringCache
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
	               (implicit exc: ExecutionContext) =
		new ExpiringCache[K, V](request)(calculateExpiration)
	
	/**
	  * Creates a new expiring cache
	  * @param expirationDuration A duration after which generated values expire
	  * @param request Function for requesting new values
	  * @param exc Implicit execution context
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new cache that clears values automatically
	  */
	def after[K, V](expirationDuration: FiniteDuration)(request: K => V)(implicit exc: ExecutionContext) =
		new ExpiringCache[K, V](request)((_, _) => expirationDuration)
}

/**
  * A cache which removes its contents a while after they have been generated
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class ExpiringCache[K, V](request: K => V)(calculateExpiration: (K, V) => Duration)
                         (implicit exc: ExecutionContext)
	extends CacheLike[K, V]
{
	// ATTRIBUTES   -----------------------------
	
	implicit val log: Logger = SysErrLogger
	
	private val waitLock = new AnyRef
	private val cachePointer = Volatile(Map[K, V]())
	private val queuedExpirationsPointer = VolatileList[(Instant, K)]()
	
	
	// IMPLEMENTED  -----------------------------
	
	override def cachedValues = cachePointer.value.values
	
	override def apply(key: K) = cached(key).getOrElse {
		// Generates and stores the new value
		val newValue = request(key)
		val expirationDuration = calculateExpiration(key, newValue)
		if (expirationDuration > Duration.Zero) {
			cachePointer.update { _ + (key -> newValue) }
			// Prepares an expiration if necessary
			expirationDuration.finite.foreach { expirationDuration =>
				// Queues a new expiration
				val expirationTime = Now + expirationDuration
				val needsNotify = queuedExpirationsPointer.pop { queue =>
					// Case: There were no other expirations queued
					if (queue.isEmpty)
						false -> Vector(expirationTime -> key)
					else
						queue.findLastIndexWhere { case (time, _) => time < expirationTime } match {
							// Case: The new expiration is executed after some other expiration =>
							// No need to modify the expiration process
							case Some(previousIndex) =>
								val newQueue = (queue.take(previousIndex + 1) :+
									(expirationTime -> key)) ++ queue.drop(previousIndex + 1)
								false -> newQueue
							// Case: The new expiration becomes the first expiration time =>
							// Notifies the expiration process of this change
							case None =>
								val newQueue = (expirationTime -> key) +: queue
								true -> newQueue
						}
				}
				// Starts asynchronous expiration process, if not already started
				ExpirationProcess.runAsync()
				// Also notifies the process if needed
				if (needsNotify)
					WaitUtils.notify(waitLock)
			}
		}
		// Finally returns the newly calculated value
		newValue
	}
	
	override def cached(key: K) = cachePointer.value.get(key)
	
	
	// NESTED   ---------------------------
	
	private object ExpirationProcess extends LoopingProcess(waitLock = waitLock)
	{
		override protected def isRestartable = true
		
		override protected def iteration() = {
			queuedExpirationsPointer.headOption.flatMap { case (waitTarget, targetKey) =>
				// Waits until the wait target is reached (may be interrupted)
				if (Wait(waitTarget, waitLock)) {
					// If target was reached, removes the key from the cache
					// Otherwise finds a new target
					if (Now >= waitTarget) {
						cachePointer.update { _ - targetKey }
						queuedExpirationsPointer.update { _.filterNot { _._2 == targetKey } }
					}
					// Schedules the next wait based on the next expiration
					queuedExpirationsPointer.headOption.map { _._1 }
				}
				else {
					markAsInterrupted()
					None
				}
			}
		}
	}
}
