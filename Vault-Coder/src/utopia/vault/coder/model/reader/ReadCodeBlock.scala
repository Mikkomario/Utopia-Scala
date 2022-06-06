package utopia.vault.coder.model.reader

import utopia.vault.coder.model.scala.code.{Code, CodeLine}
import utopia.vault.coder.model.scala.datatype.Reference

object ReadCodeBlock
{
	/**
	  * An empty code block
	  */
	val empty = apply(Vector())
	
	/**
	  * @param line A line of code (should not contain '{' or '}' at the ends)
	  * @return That line of code, wrapped in a block
	  */
	def apply(line: String): ReadCodeBlock = apply(Vector(CodeLine(line)))
}

/**
  * Represents a {block} of various unprocessed code
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  */
case class ReadCodeBlock(lines: Vector[CodeLine])
{
	/**
	  * @return Whether this is an empty block of code
	  */
	def isEmpty = lines.isEmpty
	/**
	  * @return Whether this block contains at least one line of code
	  */
	def nonEmpty = lines.nonEmpty
	
	/**
	  * @return Whether this is a single line code block
	  */
	def isSingleLine = lines.size == 1
	/**
	  * @return Whether this block spans multiple lines
	  */
	def isMultiLine = lines.size > 1
	
	/**
	  * Converts this block to code
	  * @param refMap Map containing reference targets as keys and matching references as values
	  * @return This block as code where the 0 indent level is at the declaration concerning this block
	  */
	def toCodeWith(refMap: Map[String, Reference]) =
	{
		// Case: Empty block
		if (isEmpty)
			Code.empty
		// Case: One-liner
		else if (isSingleLine)
		{
			val line = lines.head.code
			Code(line).referringTo(refMap.keySet.filter(line.contains).map(refMap.apply))
		}
		// Case: Multiple lines of code
		else
		{
			val references = refMap.keySet.filter { target => lines.exists { _.code.contains(target) } }
				.map(refMap.apply)
			Code(lines, references)
		}
	}
}
