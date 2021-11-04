package utopia.vault.coder.model.scala

import utopia.vault.coder.model.scala.ScalaDocKeyword.Param
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.declaration.DeclarationStart
import utopia.vault.coder.model.scala.template.{ScalaConvertible, ScalaDocConvertible}

/**
  * Represents a scala method parameter
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Parameter(name: String, dataType: ScalaType, default: CodePiece = CodePiece.empty,
                     prefix: Option[DeclarationStart] = None, description: String = "")
	extends ScalaConvertible with ScalaDocConvertible
{
	// IMPLEMENTED  ---------------------------
	
	override def toScala =
	{
		val defaultPart = if (default.isEmpty) default else default.withPrefix(" = ")
		val mainPart = dataType.toScala.withPrefix(name + ": ") + defaultPart
		prefix match
		{
			case Some(prefix) => prefix.toScala.append(mainPart, " ")
			case None => mainPart
		}
	}
	
	override def documentation =
		if (description.nonEmpty) Vector(ScalaDocPart(Param(name), description.linesIterator.toVector)) else Vector()
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new parameter list by adding implicits to this parameter
	  * @param firstParam First implicit parameter
	  * @param moreParams More implicit parameters
	  * @return A new parameters list
	  */
	def withImplicits(firstParam: Parameter, moreParams: Parameter*) =
		Parameters(Vector(Vector(this)), firstParam +: moreParams.toVector)
}
