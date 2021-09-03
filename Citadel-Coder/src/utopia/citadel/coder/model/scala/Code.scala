package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.template.Referencing

/**
  * Represents one or more lines of scala code
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Code(lines: Vector[String], references: Set[Reference] = Set()) extends Referencing
{
	/**
	  * @return Whether this code contains multiple lines
	  */
	def isMultiLine = lines.size > 1
	/**
	  * @return Whether this code contains a single line only
	  */
	def isSingleLine = lines.size < 2
}
