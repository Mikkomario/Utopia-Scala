package utopia.flow.collection

import utopia.flow.collection.immutable.caching.cache.Cache

/**
  * @author Mikko Hilpinen
  * @since 14/01/2024, v2.3
  */
package object template
{
	@deprecated("Deprecated for removal", "v2.3")
	type CacheLike[-K, +V] = Cache[K, V]
}
