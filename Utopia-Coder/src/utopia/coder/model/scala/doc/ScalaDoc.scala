package utopia.coder.model.scala.doc

import utopia.flow.operator.MaybeEmpty
import utopia.coder.model.scala.DeclarationDate
import utopia.coder.model.scala.code.Code
import utopia.coder.model.scala.doc.ScalaDocKeyword.{Author, Param, Return, Since}
import utopia.coder.model.scala.template.CodeConvertible

object ScalaDoc
{
	/**
	  * An empty scaladoc
	  */
	val empty = apply(Vector())
}

/**
  * Represents a scaladoc element
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  */
case class ScalaDoc(parts: Vector[ScalaDocPart]) extends CodeConvertible with MaybeEmpty[ScalaDoc]
{
	// COMPUTED ----------------------------------
	
	/**
	  * @return Main description in this scaladoc
	  */
	def description = parts.filter { _.keyword.isEmpty }.flatMap { _.content }.mkString("\n")
	/**
	  * @return Description of the function return value
	  */
	def returnDescription = apply(Return)
	/**
	  * @return Parsed since value, if available
	  */
	def since = parts.find { _.keyword.contains(Since) }.flatMap { _.content.headOption }
		.flatMap { DeclarationDate(_).toOption }
	/**
	  * @return Described item's author
	  */
	def author = apply(Author)
	
	
	// IMPLEMENTED  ------------------------------
	
	override def self = this
	
	override def isEmpty = parts.isEmpty
	
	override def toCode = {
		if (parts.isEmpty)
			Code.empty
		else {
			val partsCode = parts.map { _.toCode }.reduceLeft { _ ++ _ }
			"/**" +: partsCode.prependAll("  * ") :+ "  */"
		}
	}
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param keyword Targeted keyword
	  * @return Text content for that keyword (may be empty)
	  */
	def apply(keyword: ScalaDocKeyword) =
		parts.filter { _.keyword.contains(keyword) }.flatMap { _.content }.mkString("\n")
	
	/**
	  * @param paramName Name of the targeted parameter
	  * @return Description of that parameter
	  */
	def param(paramName: String) = apply(Param(paramName))
}
