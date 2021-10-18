package utopia.vault.coder.model.scala

import utopia.vault.coder.model.scala.ScalaDocKeyword.Param
import utopia.vault.coder.model.scala.code.CodePiece
import utopia.vault.coder.model.scala.template.{ScalaConvertible, ScalaDocConvertible}

/**
  * Represents a scala method parameter
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Parameter(name: String, dataType: ScalaType, default: CodePiece = CodePiece.empty, prefix: String = "",
                     description: String = "")
	extends ScalaConvertible with ScalaDocConvertible
{
	// IMPLEMENTED  ---------------------------
	
	override def toScala =
	{
		val prefixString = if (prefix.isEmpty) "" else s"$prefix "
		val defaultPart = if (default.isEmpty) default else default.withPrefix(" = ")
		(dataType.toScala + defaultPart).withPrefix(s"$prefixString$name: ")
	}
	
	override def documentation =
		if (description.nonEmpty) Vector(ScalaDocPart(Param, s"$name $description")) else Vector()
	
	
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
