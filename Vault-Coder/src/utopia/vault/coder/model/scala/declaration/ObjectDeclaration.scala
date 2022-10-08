package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.datatype.{Extension, GenericType}
import utopia.vault.coder.model.scala.{Annotation, DeclarationDate, Visibility}

/**
  * Used for declaring objects in scala files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ObjectDeclaration(name: String, extensions: Vector[Extension] = Vector(),
                             creationCode: Code = Code.empty, properties: Vector[PropertyDeclaration] = Vector(),
                             methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                             visibility: Visibility = Public, annotations: Seq[Annotation] = Vector(),
                             description: String = "", author: String = "",
                             headerComments: Vector[String] = Vector(), since: DeclarationDate = DeclarationDate.today,
                             isCaseObject: Boolean = false)
	extends InstanceDeclaration
{
	override def keyword = if (isCaseObject) "case object" else "object"
	
	override protected def constructorParams = None
	
	// Objects can't have generic type parameters since they're never abstract
	override def genericTypes = Vector()
	
	override protected def makeCopy(visibility: Visibility, genericTypes: Seq[GenericType],
	                                extensions: Vector[Extension], creationCode: Code,
	                                properties: Vector[PropertyDeclaration], methods: Set[MethodDeclaration],
	                                nested: Set[InstanceDeclaration], annotations: Seq[Annotation],
	                                description: String, author: String,
	                                headerComments: Vector[String], since: DeclarationDate) =
		ObjectDeclaration(name, extensions, creationCode, properties, methods, nested, visibility, annotations,
			description, author, headerComments, since, isCaseObject)
}
