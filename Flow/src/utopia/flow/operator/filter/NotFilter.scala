package utopia.flow.operator.filter

/**
  * NotFilters accept items only if the underlying filter doesn't
  * @param original The original filter
  * @tparam T Filtered item type
  * @author Mikko Hilpinen
  * @since 5.4.2019, v2+
  */
case class NotFilter[-T](original: Filter[T]) extends Filter[T]
{
	override def apply(item: T) = !original(item)
	
	override def unary_! = original
}
