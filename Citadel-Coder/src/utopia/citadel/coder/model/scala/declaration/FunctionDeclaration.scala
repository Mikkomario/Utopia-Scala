package utopia.citadel.coder.model.scala.declaration

import utopia.citadel.coder.model.scala.template.CodeConvertible
import utopia.citadel.coder.model.scala.{Code, Parameters}

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
	  * @return Parameters accepted by this function. None if this function is parameterless.
	  */
	protected def params: Option[Parameters]
	
	
	// IMPLEMENTED  --------------------------
	
	override def references = code.references ++ params.iterator.flatMap { _.references }
	
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]()
		val overridePart = if (isOverridden) "override " else ""
		val parametersString = params match {
			case Some(params) => params.toScala
			case None => ""
		}
		
		val header = s"$overridePart$baseString$parametersString = "
		if (code.isSingleLine) {
			val line = code.lines.head
			// Case: Single line function
			if (line.length + header.length < CodeConvertible.maxLineLength)
				builder += header + line
			// Case: Two-line function
			else {
				builder += header
				builder += "\t" + line
			}
		}
		// Case: Multi-line function
		else {
			builder += header + '{'
			code.lines.foreach { builder += "\t" + _ }
			builder += "}"
		}
		
		builder.result()
	}
}
