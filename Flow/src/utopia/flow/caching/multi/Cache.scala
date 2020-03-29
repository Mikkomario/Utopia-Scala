package utopia.flow.caching.multi

import scala.collection.immutable.HashMap

object Cache
{
	/**
	  * Creates a new cache
	  * @param request A function for retrieving the cached value
	  * @tparam Key The cache key type
	  * @tparam Value The type of cached result
	  * @return A new cache
	  */
	def apply[Key, Value](request: Key => Value) = new Cache[Key, Value](request)
}

/**
  * This is a simple implementation of the CacheLike trait
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
class Cache[Key, Value](private val request: Key => Value) extends CacheLike[Key, Value]
{
	// ATTRIBUTES	------------------
	
	private var cachedItems: Map[Key, Value] = HashMap()
	
	
	// IMPLEMENTED	------------------
	
	override def apply(key: Key) =
	{
		if (cachedItems.contains(key))
			cachedItems(key)
		else
		{
			val value = request(key)
			cachedItems += key -> value
			value
		}
	}
	
	override def cached(key: Key) = cachedItems.get(key)
}
