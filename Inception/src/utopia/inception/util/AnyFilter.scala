package utopia.inception.util

/**
 * This event filter will accept any kind of item
 * @author Mikko Hilpinen
 * @since 21.1.2017
 */
object AnyFilter extends Filter[Any]
{
	override def apply(item: Any) = true
}