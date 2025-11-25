package utopia.flow.collection.mutable.builder

import scala.collection.mutable

/**
 * A builder that ignores the input values. May be used as a placeholder.
 * @author Mikko Hilpinen
 * @since 25.11.2025, v2.8
 */
object NothingBuilder extends mutable.Builder[Any, Nothing]
{
	override def clear() = ()
	
	//noinspection NotImplementedCode
	override def result() = ???
	
	override def addOne(elem: Any) = this
}
