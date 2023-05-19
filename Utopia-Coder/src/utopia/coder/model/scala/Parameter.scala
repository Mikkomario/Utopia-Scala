package utopia.coder.model.scala

import utopia.coder.model.scala.doc.ScalaDocKeyword.Param
import utopia.coder.model.scala.code.CodePiece
import utopia.coder.model.scala.datatype.ScalaType
import utopia.coder.model.scala.declaration.DeclarationStart
import utopia.coder.model.scala.doc.ScalaDocPart
import utopia.coder.model.scala.template.{ScalaConvertible, Documented}

/**
  * Represents a scala method parameter
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Parameter(name: String, dataType: ScalaType, default: CodePiece = CodePiece.empty,
                     prefix: Option[DeclarationStart] = None, description: String = "")
	extends ScalaConvertible with Documented
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Whether this parameter has a default value
	  */
	def hasDefault = default.nonEmpty
	/**
	  * @return Whether this parameter doesn't have a default value
	  */
	def hasNoDefault = !hasDefault
	
	/**
	  * @return A copy of this parameter without a default value
	  */
	def withoutDefault = if (hasDefault) copy(default = CodePiece.empty) else this
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toScala =
	{
		val defaultPart = if (default.isEmpty) default else default.withPrefix(" = ")
		val mainPart = dataType.toScala.withPrefix(name + ": ") + defaultPart
		prefix match {
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
