package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.template.{Referencing, ScalaConvertible}

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
case class Extension(parentType: ScalaType, constructionAssignments: Vector[Vector[String]] = Vector(),
                     constructionReferences: Set[Reference] = Set())
	extends Referencing with ScalaConvertible
{
	// IMPLEMENTED  -------------------------------------
	
	override def references = constructionReferences ++ parentType.references
	
	override def toScala =
	{
		val parametersString = if (constructionAssignments.isEmpty) "" else
			s"(${constructionAssignments.map { assignments => s"(${assignments.mkString(", ")})"} })"
		s"${parentType.toScala}$parametersString"
	}
	
	
	// OTHER    -----------------------------------------
	
	/**
	  * Creates a copy of this extension which includes a constructor call
	  * @param references References used during this call
	  * @param paramValues Parameter value assignments
	  * @return A copy of this extension which includes a constructor call
	  */
	def withConstructor(references: Set[Reference], paramValues: String*) =
		copy(constructionAssignments = Vector(paramValues.toVector), constructionReferences = references)
}
