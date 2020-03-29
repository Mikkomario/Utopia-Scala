package utopia.flow.caching.multi

import scala.collection.immutable.HashMap
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.ref.WeakReference

object ReleasingCache
{
	/**
	  * Creates a new releasing cache
	  * @param cache An expiring cache used for requesting and releasing values
	  * @tparam Key The type of cache key
	  * @tparam Value The type of cache value
	  * @return A cache that releases references but keeps weak references
	  */
	def apply[Key, Value <: AnyRef](cache: ExpiringCacheLike[Key, Value]): ReleasingCache[Key, Value] =
		new ReleasingCacheImpl[Key, Value](cache)
	
	/**
	  * Creates a new releasing cache
	  * @param releaseAfterDuration The duration after which the resource is released
	  * @param request A function for requesting resources
	  * @tparam Key The type of key used in this cache
	  * @tparam Value The type of cached value
	  * @return A cache that strongly refers to the item for releaseAfterDuration and then weakly refers to the item
	  */
	def apply[Key, Value <: AnyRef](releaseAfterDuration: FiniteDuration)(request: Key => Value): ReleasingCache[Key, Value] =
		apply(ExpiringCache.apply(releaseAfterDuration)(request))
}

/**
  * This cache releases its references after a while
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait ReleasingCache[Key, Value <: AnyRef] extends CacheLike[Key, Value]
{
	// ATTRIBUTES	---------------
	
	private var weakRefs: Map[Key, WeakReference[Value]] = HashMap()
	
	
	// ABSTRACT	-------------------
	
	/**
	  * @return The cache that provides the values for this cache
	  */
	protected def source: ExpiringCacheLike[Key, Value]
	
	
	// IMPLEMENTED	---------------
	
	override def cached(key: Key) = source.cached(key) orElse weakRefs.get(key).flatMap { _.get }
	
	override def apply(key: Key) =
	{
		// Expires old values first
		releaseExpiredData()
		
		// Tries to use a cached or a weakly cached value
		cached(key).getOrElse
		{
			// But may have to request a new value
			val newValue = source(key)
			weakRefs += (key -> WeakReference(newValue))
			newValue
		}
	}
	
	
	// OTHER	------------------
	
	/**
	  * Releases expired strong references
	  */
	def releaseExpiredData() = source.clearExpiredData()
	
	/**
	  * Clears all strong references, expired or not
	  */
	def clearStrongReferences() = source.clear()
	
	/**
	  * Clears all references, both strong and weak, expired and non-expired
	  */
	def clearAllReferences() =
	{
		clearStrongReferences()
		weakRefs = HashMap()
	}
	
	/**
	  * @param key A key
	  * @return Whether there is currently a strong reference to the specified key
	  */
	def isStronglyReferenced(key: Key) = source.isValueCached(key)
}

private class ReleasingCacheImpl[Key, Value <: AnyRef](protected val source: ExpiringCacheLike[Key, Value])
	extends ReleasingCache[Key, Value]