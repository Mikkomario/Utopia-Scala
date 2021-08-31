package utopia.citadel.coder.model.scala

import scala.collection.immutable.VectorBuilder

/**
  * Declares a scala method or a property of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait FunctionDeclaration extends Declaration with CodeConvertible
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Code that forms this method / property
	  */
	def code: Code
	
	/**
	  * @return Whether this declaration overrides a base declaration
	  */
	def isOverridden: Boolean
	
	/**
	  * @return A string representation of the parameters of this function
	  */
	def parametersString: String
	
	
	// IMPLEMENTED  --------------------------
	
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]()
		// Adds the override annotation if necessary
		if (isOverridden)
			builder += "@Override"
		
		val header = s"$baseString$parametersString = "
		if (code.isSingleLine)
		{
			val line = code.lines.head
			// Case: Single line function
			if (line.length + header.length < CodeConvertible.maxLineLength)
				builder += header + line
			// Case: Two-line function
			else
			{
				builder += header
				builder += "\t" + line
			}
		}
		// Case: Multi-line function
		else
		{
			builder += header + '{'
			code.lines.foreach { builder += "\t" + _ }
			builder += "}"
		}
		
		builder.result()
	}
}
