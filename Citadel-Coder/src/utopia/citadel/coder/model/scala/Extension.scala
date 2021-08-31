package utopia.citadel.coder.model.scala

/**
  * Represents a class extension declaration
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Extension(parentType: Reference, constructionAssignments: Vector[(String, ScalaType)] = Vector())
	extends Referencing with ScalaConvertible
{
	override def references = constructionAssignments.flatMap { _._2.references }.toSet + parentType
	
	override def toScala =
	{
		val parametersString =
			if (constructionAssignments.isEmpty) "" else s"(${constructionAssignments.map { _._1 }.mkString(", ")})"
		s"${parentType.target}$parametersString"
	}
}
