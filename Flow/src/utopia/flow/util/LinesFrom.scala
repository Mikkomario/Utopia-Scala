package utopia.flow.util

/**
 * Used for reading text lines from various sources
 * @author Mikko Hilpinen
 * @since 1.11.2019, v1.6.1+
 */
object LinesFrom extends SourceParser[Vector[String]]
{
	override protected def process(linesIterator: Iterator[String]) = linesIterator.toVector
}
