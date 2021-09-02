package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.Visibility.Public

/**
  * Used for declaring traits
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
case class TraitDeclaration(name: String, extensions: Vector[Extension] = Vector(),
                            properties: Vector[PropertyDeclaration] = Vector(),
                            methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                            visibility: Visibility = Public)
	extends InstanceDeclaration
{
	override protected def constructorParams = None
	
	override def creationCode = None
	
	override def keyword = "trait"
}
