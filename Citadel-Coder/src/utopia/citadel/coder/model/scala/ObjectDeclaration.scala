package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.Visibility.Public

/**
  * Used for declaring objects in scala files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ObjectDeclaration(name: String, extensions: Vector[Extension] = Vector(),
                             creationCode: Option[Code] = None, properties: Vector[PropertyDeclaration] = Vector(),
                             methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                             visibility: Visibility = Public)
	extends InstanceDeclaration
{
	override def keyword = "object"
	
	override def references = (extensions ++ creationCode ++ properties ++ methods)
		.flatMap { _.references }.toSet
	
	override def parametersString = ""
}
