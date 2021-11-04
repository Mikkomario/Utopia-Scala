package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.Visibility
import utopia.vault.coder.model.scala.code.CodePiece

/**
  * Declares a scala item of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait Declaration
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return Visibility of this declaration
	  */
	def visibility: Visibility
	/**
	  * @return Keyword for this declaration (E.g. "def" or "val")
	  */
	def keyword: CodePiece
	/**
	  * @return Name of this method / property
	  */
	def name: String
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The base part of this declaration as a string. E.g. "private def test"
	  */
	protected def basePart =
	{
		val visibilityPart = visibility.toScala
		val otherPart = keyword + s" $name"
		if (visibilityPart.isEmpty)
			otherPart
		else
			visibility.toScala + otherPart.withPrefix(" ")
	}
}
