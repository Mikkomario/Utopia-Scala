package utopia.coder.model.scala.doc

import utopia.flow.parse.string.Regex
import utopia.coder.model.scala.code.Code
import utopia.coder.model.scala.template.CodeConvertible

object ScalaDocPart
{
	/**
	  * @param keyword Scaladoc keyword
	  * @param content Documentation
	  * @return A new scaladoc part
	  */
	def apply(keyword: ScalaDocKeyword, content: Vector[String]): ScalaDocPart = apply(content, Some(keyword))
	/**
	  * @param keyword Scaladoc keyword
	  * @param firstLine First line of documentation
	  * @param moreLines More lines of documentation
	  * @return A new scaladoc part
	  */
	def apply(keyword: ScalaDocKeyword, firstLine: String, secondLine: String, moreLines: String*): ScalaDocPart =
		apply(Vector(firstLine, secondLine) ++ moreLines, Some(keyword))
	/**
	  * @param keyword Scaladoc keyword
	  * @param content Documentation content. May contain multiple lines.
	  * @return A new scaladoc part
	  */
	def apply(keyword: ScalaDocKeyword, content: String): ScalaDocPart = apply(contentToLines(content), Some(keyword))
	
	/**
	  * Creates a scaladoc part without keyword (the general description)
	  * @param firstLine First document line
	  * @param secondLine Second document line
	  * @param moreLines More document lines
	  * @return A new scaladoc part
	  */
	def description(firstLine: String, secondLine: String, moreLines: String*): ScalaDocPart =
		apply(Vector(firstLine, secondLine) ++ moreLines, None)
	/**
	  * Creates a scaladoc part without keyword (the general description)
	  * @param content Documentation as a string (may contain multiple lines)
	  * @return A new scaladoc part
	  */
	def description(content: String) = apply(contentToLines(content), None)
	
	private def contentToLines(content: String) =
		content.linesIterator.map(Regex.newLine.filterNot).filterNot { _.forall { _ == ' ' } }.toVector
}

/**
  * Represents a portion of a scaladoc
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
case class ScalaDocPart(content: Vector[String], keyword: Option[ScalaDocKeyword]) extends CodeConvertible
{
	override def toCode = {
		if (content.isEmpty)
			Code.empty
		else
			keyword match {
				case Some(keyword) => Code.from(s"$keyword ${content.head}" +: content.tail)
				case None => Code.from(content)
			}
	}
}
