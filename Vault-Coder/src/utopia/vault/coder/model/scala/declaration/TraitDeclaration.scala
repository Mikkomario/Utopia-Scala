package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.datatype.{Extension, GenericType}
import utopia.vault.coder.model.scala.{DeclarationDate, Visibility}

/**
  * Used for declaring traits
  * @author Mikko Hilpinen
  * @since 2.9.2021, v0.1
  */
case class TraitDeclaration(name: String, genericTypes: Seq[GenericType] = Vector(),
                            extensions: Vector[Extension] = Vector(),
                            properties: Vector[PropertyDeclaration] = Vector(),
                            methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                            visibility: Visibility = Public, description: String = "", author: String = "",
                            headerComments: Vector[String] = Vector(), since: DeclarationDate = DeclarationDate.today,
                            isSealed: Boolean = false)
	extends InstanceDeclaration
{
	override protected def constructorParams = None
	
	override def creationCode = Code.empty
	
	override def keyword = if (isSealed) "sealed trait" else "trait"
	
	override protected def makeCopy(visibility: Visibility, genericTypes: Seq[GenericType],
	                                extensions: Vector[Extension], creationCode: Code,
	                                properties: Vector[PropertyDeclaration], methods: Set[MethodDeclaration],
	                                nested: Set[InstanceDeclaration], description: String, author: String,
	                                headerComments: Vector[String], since: DeclarationDate) =
		TraitDeclaration(name, genericTypes, extensions, properties, methods, nested, visibility, description, author,
			headerComments, since, isSealed)
}
