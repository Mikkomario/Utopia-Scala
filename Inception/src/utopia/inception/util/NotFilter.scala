package utopia.inception.util

object NotFilter
{
	/**
	  * @param filter A filter
	  * @tparam T The type of filtered item
	  * @return A filter that accepts an item only if the provided filter doesn't
	  */
	def apply[T](filter: Filter[T]) = new NotFilter(filter)
}

/**
  * NotFilters accept items only if the underlying filter doesn't
  * @param original The original filter
  * @tparam T Filtered item type
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
class NotFilter[-T](original: Filter[T]) extends Filter[T]
{
	override def apply(item: T) = !original(item)
	
	override def unary_! = original
}
