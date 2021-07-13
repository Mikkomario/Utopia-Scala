package utopia.flow.caching.single

import scala.concurrent.duration.FiniteDuration

/**
  * This cache may be cleared
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
@deprecated("Please use ResettableLazy instead", "v1.10")
trait ClearableSingleCacheLike[+A] extends SingleCacheLike[A]
{
	// ABSTRACT	-----------------
	
	/**
	  * Clears this cache, which means that a new value will be requested afterwards
	  */
	def clear(): Unit
	
	
	// OTHER	----------------
	
	/**
	  * @param cacheDuration The duration after which this cache's item should be considered expired
	  * @return An expiring version of this cache
	  */
	def expiring(cacheDuration: FiniteDuration) = ExpiringSingleCache.wrap(this, cacheDuration)
}
