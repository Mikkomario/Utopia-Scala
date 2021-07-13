package utopia.flow.caching.multi

import utopia.flow.caching.single.ExpiringSingleCacheLike

/**
  * This cache holds values which expire after a while
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
@deprecated("Please use ExpiringMultiLazyLike instead", "v1.10")
trait ExpiringCacheLike[-Key, +Value] extends MultiCacheLike[Key, Value, ExpiringSingleCacheLike[Value]]
{
	// ABSTRACT	--------------------
	
	/**
	  * @return The currently used caches
	  */
	protected def currentCaches: Iterable[ExpiringSingleCacheLike[Value]]
	
	
	// IMPLEMENTED	----------------
	
	override def apply(key: Key) =
	{
		// Clears expired data
		clearExpiredData()
		
		// Retrieves current data
		super.apply(key)
	}
	
	
	// OTHER	---------------------
	
	/**
	  * Clears all expired values from this cache
	  */
	def clearExpiredData() = currentCaches.foreach { _.clearIfExpired() }
	
	/**
	  * Clears all values from this cache, expired or not
	  */
	def clear() = currentCaches.foreach { _.clear() }
}
