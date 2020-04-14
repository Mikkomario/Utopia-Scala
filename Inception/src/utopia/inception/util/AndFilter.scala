package utopia.inception.util

object AndFilter
{
	/**
	  * @param first The first filter
	  * @param second The second filter
	  * @param more More filters
	  * @tparam T Type of filtered item
	  * @return A filter that only accepts item when all of the provided filters do
	  */
	def apply[T](first: Filter[T], second: Filter[T], more: Filter[T]*) = new AndFilter(Vector(first, second) ++ more)
}

/**
 * This event filter only accepts event accepted by all of the included filters
 * @author Mikko Hilpinen
 * @since 21.1.2017
 */
class AndFilter[-T](val filters: Iterable[Filter[T]]) extends Filter[T]
{
	override def apply(item: T) = filters.forall { _(item) }
}