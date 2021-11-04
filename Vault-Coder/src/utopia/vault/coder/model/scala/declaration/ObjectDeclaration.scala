package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.{Extension, Visibility}

/**
  * Used for declaring objects in scala files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ObjectDeclaration(name: String, extensions: Vector[Extension] = Vector(),
                             creationCode: Code = Code.empty, properties: Vector[PropertyDeclaration] = Vector(),
                             methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                             visibility: Visibility = Public, description: String = "", author: String = "",
                             headerComments: Vector[String] = Vector(), isCaseObject: Boolean = false)
	extends InstanceDeclaration
{
	override def keyword = if (isCaseObject) "case object" else "object"
	
	override protected def constructorParams = None
	
	override protected def makeCopy(visibility: Visibility, extensions: Vector[Extension], creationCode: Code,
	                                properties: Vector[PropertyDeclaration], methods: Set[MethodDeclaration],
	                                nested: Set[InstanceDeclaration], description: String, author: String,
	                                headerComments: Vector[String]) =
		ObjectDeclaration(name, extensions, creationCode, properties, methods, nested, visibility, description,
			author, headerComments, isCaseObject)
}
