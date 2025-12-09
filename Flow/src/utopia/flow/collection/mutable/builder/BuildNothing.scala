package utopia.flow.collection.mutable.builder

import utopia.flow.collection.immutable.Empty

import scala.collection.mutable

object BuildNothing
{
	/**
	 * Builds nothing. Yields Unit.
	 */
	lazy val unit = new BuildNothing[Unit](())
	/**
	 * Builds nothing. Yields an empty collection.
	 */
	lazy val empty = new BuildNothing[Empty.type](Empty)
}

/**
 * A builder that ignores the input values. May be used as a placeholder.
 * @author Mikko Hilpinen
 * @since 25.11.2025, v2.8
 */
class BuildNothing[+A](fixedResult: A) extends mutable.Builder[Any, A]
{
	override def clear() = ()
	override def result() = fixedResult
	
	override def addOne(elem: Any) = this
}
