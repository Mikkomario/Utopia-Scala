package utopia.citadel.coder.model.scala

/**
  * Represents a class extension declaration
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class Extension(parentType: Reference, constructionAssignments: Vector[(String, ScalaType)] = Vector())
	extends Referencing
{
	override def references = constructionAssignments.flatMap { _._2.references }.toSet + parentType
}
