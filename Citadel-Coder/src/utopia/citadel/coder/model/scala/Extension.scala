package utopia.citadel.coder.model.scala

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
case class Extension(parentType: ScalaType, constructionAssignments: Vector[(String, ScalaType)] = Vector())
	extends Referencing with ScalaConvertible
{
	override def references = constructionAssignments.flatMap { _._2.references }.toSet ++ parentType.references
	
	override def toScala =
	{
		val parametersString =
			if (constructionAssignments.isEmpty) "" else s"(${constructionAssignments.map { _._1 }.mkString(", ")})"
		s"${parentType.toScala}$parametersString"
	}
}
