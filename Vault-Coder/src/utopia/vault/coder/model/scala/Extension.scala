package utopia.vault.coder.model.scala

import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.template.ScalaConvertible

import scala.language.implicitConversions

object Extension
{
	// Implicitly converts scala types and references to extensions
	implicit def referenceToExtension(ref: Reference): Extension = apply(ref)
	implicit def typeToExtension(ref: ScalaType): Extension = apply(ref)
}

/**
  * Represents a class extension declaration
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Extension(parentType: ScalaType, constructionAssignments: Vector[Vector[CodePiece]] = Vector())
	extends ScalaConvertible
{
	// IMPLEMENTED  -------------------------------------
	
	override def toScala =
	{
		val parametersPart = if (constructionAssignments.isEmpty) CodePiece.empty else
			constructionAssignments
				.map { _.reduceLeftOption { _.append(_, ", ") }.getOrElse(CodePiece.empty)
					.withinParenthesis }
				.reduceLeft { _ + _ }
		parentType.toScala + parametersPart
	}
	
	
	// OTHER    -----------------------------------------
	
	/**
	  * Creates a copy of this extension which includes a constructor call
	  * @param assignments Parameter assignments (a single parameter list)
	  * @return A copy of this extension which includes a constructor call
	  */
	def withConstructor(assignments: Vector[CodePiece]) =
		copy(constructionAssignments = constructionAssignments :+ assignments)
	
	/**
	  * Creates a copy of this extension which includes a constructor call (one more parameter list)
	  * @param firstAssignment First parameter assignment
	  * @param moreAssignments More parameter assignments
	  * @return A copy of this extension which includes a constructor call
	  */
	def withConstructor(firstAssignment: CodePiece, moreAssignments: CodePiece*) =
		copy(constructionAssignments = constructionAssignments :+ (firstAssignment +: moreAssignments.toVector))
}
