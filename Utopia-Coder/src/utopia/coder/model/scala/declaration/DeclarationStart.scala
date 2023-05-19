package utopia.coder.model.scala.declaration

import utopia.coder.model.scala.Visibility
import utopia.coder.model.scala.Visibility.Public
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.declaration.DeclarationPrefix.Override
import utopia.coder.model.scala.declaration.FunctionDeclarationType.ValueD
import utopia.coder.model.scala.template.ScalaConvertible

object DeclarationStart
{
	lazy val overrideVal = apply(ValueD, prefixes = Vector(Override))
}

/**
  * Contains the keywords used when declaring something (a class, a property etc.)
  * @author Mikko Hilpinen
  * @since 1.11.2021, v1.3
  * @param declarationType Type of this declaration
  * @param visibility Visibility of the declared item (default = public)
  * @param prefixes Custom prefixes to apply (default = empty)
  */
case class DeclarationStart(declarationType: DeclarationType, visibility: Visibility = Public,
                            prefixes: Vector[DeclarationPrefix] = Vector())
	extends ScalaConvertible
{
	override def toScala =
	{
		val mainPart = if (visibility == Public) CodePiece(declarationType.keyword) else
			visibility.toScala.append(declarationType.keyword, " ")
		
		if (prefixes.isEmpty)
			mainPart
		else
			prefixes.map { _.toScala }.reduceLeft { _.append(_, ", ") }
				.append(mainPart, " ")
	}
}