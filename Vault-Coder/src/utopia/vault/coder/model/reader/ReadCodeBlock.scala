package utopia.vault.coder.model.reader

object ReadCodeBlock
{
	/**
	  * @param line A line of code (should not contain '{' or '}' at the ends)
	  * @return That line of code, wrapped in a block
	  */
	def apply(line: String): ReadCodeBlock = apply(Vector(line))
}

/**
  * Represents a {block} of various unprocessed code
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  */
case class ReadCodeBlock(lines: Vector[String])
