package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.template.CodeConvertible

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
	def apply(keyword: ScalaDocKeyword, firstLine: String, moreLines: String*): ScalaDocPart =
		apply(firstLine +: moreLines.toVector, Some(keyword))
	
	/**
	  * Creates a scaladoc part without keyword (the general description)
	  * @param firstLine First document line
	  * @param moreLines More document lines
	  * @return A new scaladoc part
	  */
	def description(firstLine: String, moreLines: String*): ScalaDocPart = apply(firstLine +: moreLines.toVector, None)
}

/**
  * Represents a portion of a scaladoc
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
case class ScalaDocPart(content: Vector[String], keyword: Option[ScalaDocKeyword]) extends CodeConvertible
{
	override def toCodeLines =
	{
		if (content.isEmpty)
			Vector()
		else
			keyword match
			{
				case Some(keyword) => s"$keyword ${content.head}" +: content.tail
				case None => content
			}
	}
}
