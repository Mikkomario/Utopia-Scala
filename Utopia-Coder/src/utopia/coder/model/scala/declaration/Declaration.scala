package utopia.coder.model.scala.declaration

import utopia.coder.model.scala.{Annotation, Visibility}
import utopia.coder.model.scala.code.{Code, CodeLine, CodePiece}
import utopia.coder.model.scala.datatype.GenericType
import utopia.coder.model.scala.template.{CodeConvertible, Documented}

/**
  * Declares a scala item of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait Declaration extends CodeConvertible with Documented
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
	
	/**
	  * @return Annotations that apply to this (whole) declaration
	  */
	def annotations: Seq[Annotation]
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The annotations part of this declaration's code, which typically appears between the base part and
	  *         the scaladoc part.
	  */
	protected def annotationsPart = {
		if (annotations.isEmpty)
			Code.empty
		else {
			val pieces = annotations.map { _.toScala }
			Code(pieces.map { p => CodeLine(p.text) }.toVector, pieces.flatMap { _.references }.toSet)
		}
	}
	/**
	  * @return The base part of this declaration as a string. E.g. "private def test"
	  */
	protected def basePart = {
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
