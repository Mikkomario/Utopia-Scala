package utopia.vault.coder.model.scala.template

import utopia.vault.coder.controller.writer.CodeBuilder
import utopia.vault.coder.model.scala.code.{Code, CodeLine}
import utopia.vault.coder.model.scala.doc.ScalaDocPart

/**
  * Common trait for instances that can be converted to scaladoc lines
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
trait Documented
{
	// ABSTRACT --------------------------
	
	/**
	  * @return scaladoc based on this item
	  */
	def documentation: Vector[ScalaDocPart]
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return A full scaladoc for this item
	  */
	def scalaDoc: Code =
	{
		// Adds the documentation first
		val documentationCode = documentation.map { _.toCode }.filter { _.nonEmpty }
			.map { _.split.mapLines { line => CodeLine("  * " + line.code) } }
		if (documentationCode.nonEmpty)
		{
			val builder = new CodeBuilder()
			builder += "/**"
			documentationCode.foreach { builder ++= _ }
			builder += "  */"
			builder.result()
		}
		else
			Code.empty
	}
}