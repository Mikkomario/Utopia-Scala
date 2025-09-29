package utopia.flow.parse.string

import scala.io.Source

/**
  * This object contains some utility methods for producing / reading strings
  * @author Mikko Hilpinen
  * @since 1.11.2019, v1.6.1+
  */
object StringFrom extends FromSource[String, String]
{
	// IMPLEMENTED  ------------------------
	
	override protected def open: OpenSource[String] = Open
	override protected def buffer(input: String): String = input
	
	
	// NESTED   ----------------------------
	
	private object Open extends OpenSource[String]
	{
		override protected def presentSource[A](source: Source, processor: String => A): A = processor(source.mkString)
	}
}
