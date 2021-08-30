package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.Visibility.Public

/**
  * Used for declaring scala classes
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ClassDeclaration(name: String, constructionParams: Vector[Parameter] = Vector(),
                            extensions: Vector[Extension] = Vector(),
                            creationCode: Option[Code] = None,
                            properties: Set[PropertyDeclaration] = Set(),
                            methods: Set[MethodDeclaration] = Set(), visibility: Visibility = Public)
	extends InstanceDeclaration
{
	override def keyword = "class"
	
	override def references = (constructionParams ++ extensions ++ creationCode ++ properties ++ methods)
		.flatMap { _.references }.toSet
}
