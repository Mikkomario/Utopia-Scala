package utopia.inception.util

object OrFilter
{
	/**
	  * @param first A filter
	  * @param second Another filter
	  * @param more More filters
	  * @tparam T Filtered type
	  * @return A filter that accepts an item if any of the provided filters does
	  */
	def apply[T](first: Filter[T], second: Filter[T], more: Filter[T]*) = new OrFilter(Vector(first, second) ++ more)
}

/**
 * This filter uses multiple filters creating a logical OR statement. The filter accepts an 
 * event if any of the included filters accepts it
 * @author Mikko Hilpinen
 * @since 17.10.2016
 */
class OrFilter[-T](val filters: Traversable[Filter[T]]) extends Filter[T]
{
	override def apply(item: T) = filters.exists { _(item) }
}