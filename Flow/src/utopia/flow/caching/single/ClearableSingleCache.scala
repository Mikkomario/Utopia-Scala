package utopia.flow.caching.single

object ClearableSingleCache
{
	/**
	  * Creates a new clearable single cache
	  * @param request A function for requesting the item
	  * @tparam A The type of item returned
	  * @return A new cache that may be cleared
	  */
	def apply[A](request: => A) = new ClearableSingleCache[A](() => request)
}

/**
  * This single item cache may be cleared
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
class ClearableSingleCache[A](private val request: () => A) extends ClearableSingleCacheLike[A]
{
	// ATTRIBUTES	---------------
	
	private var _cached: Option[A] = None
	
	
	// IMPLEMENTED	---------------
	
	override def cached = _cached
	
	override def clear() = _cached = None
	
	override def apply() = if (isValueCached) cached.get else
	{
		val value = request()
		_cached = Some(value)
		value
	}
}
