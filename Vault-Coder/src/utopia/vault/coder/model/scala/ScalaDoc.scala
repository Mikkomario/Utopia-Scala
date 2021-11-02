package utopia.vault.coder.model.scala

import utopia.vault.coder.model.scala.ScalaDocKeyword.{Author, Param, Return, Since}
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.template.CodeConvertible

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
case class ScalaDoc(parts: Vector[ScalaDocPart]) extends CodeConvertible
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
	  * @return Since description
	  */
	def since = apply(Since)
	/**
	  * @return Described item's author
	  */
	def author = apply(Author)
	
	
	// IMPLEMENTED  ------------------------------
	
	override def toCode =
	{
		if (parts.isEmpty)
			Code.empty
		else
		{
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
