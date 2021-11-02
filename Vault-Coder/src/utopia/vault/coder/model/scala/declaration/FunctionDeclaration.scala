package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.controller.CodeBuilder
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.ScalaDocKeyword.Return
import utopia.vault.coder.model.scala.{Parameters, ScalaDocPart, ScalaType}
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
	def bodyCode: Code
	/**
	  * @return Comments presented above this function declaration, but not included in the scaladoc
	  */
	def headerComments: Vector[String]
	
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
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return Whether this function is abstract (doesn't specify a body)
	  */
	def isAbstract = bodyCode.isEmpty
	
	
	// IMPLEMENTED  --------------------------
	
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
	
	override def toCode =
	{
		val builder = new CodeBuilder()
		// Adds the documentation first
		builder ++= scalaDoc
		// Then possible header comments
		headerComments.foreach { c => builder += s"// $c" }
		// Then the header and body
		if (isOverridden)
			builder.appendPartial("override ")
		builder += basePart
		params.foreach { builder += _.toScala }
		explicitOutputType.foreach { outputType => builder.appendPartial(outputType.toScala, ": ") }
		
		// Case: Concrete function
		if (bodyCode.nonEmpty)
		{
			builder.appendPartial(" = ")
			if (bodyCode.isSingleLine)
			{
				builder.appendPartial(bodyCode.lines.head.code, allowLineSplit = true)
				builder.addReferences(bodyCode.references)
			} // Case: Multi-line function
			else
				builder.addBlock(bodyCode)
		}
		
		builder.result()
	}
}
