package utopia.flow.parse.string

/**
  * This object contains some utility methods for producing / reading strings
  * @author Mikko Hilpinen
  * @since 1.11.2019, v1.6.1+
  */
object StringFrom extends SourceParser[String]
{
	override protected def process(linesIterator: Iterator[String]) = linesIterator.mkString("\n")
}
