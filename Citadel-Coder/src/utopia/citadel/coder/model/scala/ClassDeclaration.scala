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
                            properties: Vector[PropertyDeclaration] = Vector(),
                            methods: Set[MethodDeclaration] = Set(), nested: Vector[InstanceDeclaration] = Vector(),
                            visibility: Visibility = Public, isCaseClass: Boolean = false)
	extends InstanceDeclaration
{
	override def keyword = if (isCaseClass) "case class" else "class"
	
	override def references = (constructionParams ++ extensions ++ creationCode ++ properties ++ methods)
		.flatMap { _.references }.toSet
	
	override def parametersString = s"(${constructionParams.map { _.toScala }.mkString(", ")})"
}
