package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.ScalaDocKeyword.Return
import utopia.vault.coder.model.scala.{Code, Parameters, ScalaDocPart, ScalaType}
import utopia.vault.coder.model.scala.template.{CodeConvertible, ScalaDocConvertible}

import scala.collection.immutable.VectorBuilder

/**
  * Declares a scala method or a property of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait FunctionDeclaration extends Declaration with CodeConvertible with ScalaDocConvertible
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
	  * @return Documentation describing this function (may be empty)
	  */
	def description: String
	/**
	  * @return Documentation describing the return value of this function (may be empty)
	  */
	def returnDescription: String
	
	/**
	  * @return Data type explicitly returned by this function
	  */
	def explicitOutputType: Option[ScalaType]
	/**
	  * @return Parameters accepted by this function. None if this function is parameterless.
	  */
	protected def params: Option[Parameters]
	
	
	// IMPLEMENTED  --------------------------
	
	override def references = code.references ++ params.iterator.flatMap { _.references } ++
		explicitOutputType.iterator.flatMap { _.references }
	
	override def documentation =
	{
		val desc = description
		val returnDesc = returnDescription
		val resultBuilder = new VectorBuilder[ScalaDocPart]()
		if (desc.nonEmpty)
			resultBuilder += ScalaDocPart.description(desc)
		params.foreach { resultBuilder ++= _.documentation }
		if (returnDesc.nonEmpty)
			resultBuilder += ScalaDocPart(Return, returnDesc)
		resultBuilder.result()
	}
	
	override def toCodeLines =
	{
		val builder = new VectorBuilder[String]()
		// Adds the documentation first
		builder ++= scalaDoc
		// Then the header and body
		val overridePart = if (isOverridden) "override " else ""
		val parametersString = params match {
			case Some(params) => params.toScala
			case None => ""
		}
		val dataTypeString = explicitOutputType match
		{
			case Some(dataType) => s": ${dataType.toScala}"
			case None => ""
		}
		val header = s"$overridePart$baseString$parametersString$dataTypeString = "
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
