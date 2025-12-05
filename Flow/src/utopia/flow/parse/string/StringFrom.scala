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
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param maxCharacters Maximum characters to read
	 * @return A copy of this interface only reading strings up to 'maxCharacters' length
	 */
	def take(maxCharacters: Int): FromSource[String, String] = new TakeStringFrom(maxCharacters)
	
	
	// NESTED   ----------------------------
	
	private object Open extends OpenSource[String]
	{
		override protected def presentSource[A](source: Source, processor: String => A): A = processor(source.mkString)
	}
	
	private class TakeStringFrom(maxCharacters: Int) extends FromSource[String, String]
	{
		override protected val open: OpenSource[String] = new OpenPart(maxCharacters)
		
		override protected def buffer(input: String): String = input
	}
	
	private class OpenPart(maxCharacters: Int) extends OpenSource[String]
	{
		override protected def presentSource[A](source: Source, processor: String => A): A =
			processor(source.take(maxCharacters).mkString)
	}
}
