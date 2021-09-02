package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.Visibility.Public

/**
  * Used for declaring scala classes
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ClassDeclaration(name: String, constructionParams: Parameters = Parameters.empty,
                            extensions: Vector[Extension] = Vector(),
                            creationCode: Option[Code] = None,
                            properties: Vector[PropertyDeclaration] = Vector(),
                            methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                            visibility: Visibility = Public, isCaseClass: Boolean = false)
	extends InstanceDeclaration
{
	override def keyword = if (isCaseClass) "case class" else "class"
	
	override protected def constructorParams = Some(constructionParams)
}
