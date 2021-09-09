package utopia.vault.coder.model.scala.template

import utopia.vault.coder.model.scala.ScalaDocPart

/**
  * Common trait for instances that can be converted to scaladoc lines
  * @author Mikko Hilpinen
  * @since 3.9.2021, v0.1
  */
trait ScalaDocConvertible
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
	def scalaDoc =
	{
		// Adds the documentation first
		val documentationLines = documentation.flatMap { _.toCodeLines }
		if (documentationLines.nonEmpty)
			"/**" +: documentationLines.map { line => "  * " + line} :+  "  */"
		else
			Vector()
	}
}
