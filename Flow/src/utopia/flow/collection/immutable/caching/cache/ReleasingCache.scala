package utopia.flow.collection.immutable.caching.cache

import utopia.flow.collection.template.CacheLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.ref.WeakReference

object ReleasingCache
{
	/**
	  * Creates a new cache that uses weak references after a time threshold
	  * @param request Function for acquiring new values
	  * @param calculateReferenceLength Function for calculating the length of time to hold a strong reference.
	  *                                 Accepts both key and value.
	  * @param exc Implicit execution context
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new cache
	  */
	def apply[K, V <: AnyRef](request: K => V)
	                         (calculateReferenceLength: (K, V) => Duration)
	                         (implicit exc: ExecutionContext) =
		new ReleasingCache[K, V](request)(calculateReferenceLength)
	
	/**
	  * Creates a new cache that uses weak references after a time threshold
	  * @param referenceLength Length of time for which strong references are used
	  * @param request Function for acquiring new values
	  * @param exc Implicit execution context
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new cache
	  */
	def after[K, V <: AnyRef](referenceLength: FiniteDuration)
	                         (request: K => V)
	                         (implicit exc: ExecutionContext) =
		new ReleasingCache[K, V](request)((_, _) => referenceLength)
}

/**
  * A cache that holds generated items with strong references for a while, after which weak references are
  * used as long as they are available
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class ReleasingCache[K, V <: AnyRef](request: K => V)(calculateReferenceLength: (K, V) => Duration)
                                    (implicit exc: ExecutionContext)
	extends CacheLike[K, V]
{
	// ATTRIBUTES   ------------------------------
	
	private val cache = ExpiringCache(request)(calculateReferenceLength)
	
	private var weakReferences = Map[K, WeakReference[V]]()
	
	
	// IMPLEMENTED  ------------------------------
	
	override def cachedValues = cache.cachedValues ++ weakReferences.values.flatMap { _.get }
	
	override def apply(key: K) = cached(key).getOrElse {
		// Acquires a new value and stores a weak reference to it
		val newValue = cache(key)
		weakReferences += (key -> WeakReference(newValue))
		// Returns the newly acquired value
		newValue
	}
	
	override def cached(key: K) = cache.cached(key).orElse { weak(key) }
	
	
	// OTHER    ----------------------------------
	
	private def weak(key: K) = weakReferences.get(key).flatMap { _.get }
}
