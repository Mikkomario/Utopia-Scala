package utopia.citadel.coder.model.scala.declaration

import utopia.citadel.coder.model.scala.Visibility.Public
import utopia.citadel.coder.model.scala.{Code, Extension, Visibility}

/**
  * Used for declaring objects in scala files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ObjectDeclaration(name: String, extensions: Vector[Extension] = Vector(),
                             creationCode: Option[Code] = None, properties: Vector[PropertyDeclaration] = Vector(),
                             methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                             visibility: Visibility = Public, description: String = "")
	extends InstanceDeclaration
{
	override def keyword = "object"
	
	override protected def constructorParams = None
}
