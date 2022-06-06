package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.Visibility
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.datatype.GenericType

/**
  * Declares a scala item of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait Declaration
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Visibility of the declared item
	  */
	def visibility: Visibility
	/**
	  * @return Keyword for this declaration (E.g. "def", "class" or "val")
	  */
	def keyword: CodePiece
	/**
	  * @return Name of the declared item
	  */
	def name: String
	/**
	  * @return Generic types used within this declaration
	  */
	def genericTypes: Seq[GenericType]
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The base part of this declaration as a string. E.g. "private def test"
	  */
	protected def basePart =
	{
		val visibilityPart = visibility.toScala
		val mainPart = keyword + s" $name"
		val genericPart = {
			if (genericTypes.isEmpty)
				CodePiece.empty
			else
				genericTypes.map { _.toScala }.reduceLeft { _.append(_, ", ") }.withinSquareBrackets
		}
		visibilityPart.append(mainPart, " ") + genericPart
	}
}
