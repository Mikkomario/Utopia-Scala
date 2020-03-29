package utopia.flow.caching.single

/**
  * Single cache -like items cache values when first requested
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait SingleCacheLike[+A]
{
	// ABSTRACT	-------------------
	
	/**
	  * @return The currently cached value. None if no value is currently cached
	  */
	def cached: Option[A]
	
	/**
	  * @return The value currently cached in this cache. Should cache one if there is no already
	  */
	def apply(): A
	
	
	// COMPUTED	-------------------
	
	/**
	  * @return Whether there is currently a value cached in this cache
	  */
	def isValueCached = cached.isDefined
}
