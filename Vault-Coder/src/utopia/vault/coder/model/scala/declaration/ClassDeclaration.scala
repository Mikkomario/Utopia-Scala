package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.{DeclarationDate, Extension, Parameters, Visibility}

/**
  * Used for declaring scala classes
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class ClassDeclaration(name: String, constructionParams: Parameters = Parameters.empty,
                            extensions: Vector[Extension] = Vector(),
                            creationCode: Code = Code.empty, properties: Vector[PropertyDeclaration] = Vector(),
                            methods: Set[MethodDeclaration] = Set(), nested: Set[InstanceDeclaration] = Set(),
                            visibility: Visibility = Public, description: String = "", author: String = "",
                            headerComments: Vector[String] = Vector(), since: DeclarationDate = DeclarationDate.today,
                            isCaseClass: Boolean = false)
	extends InstanceDeclaration
{
	override val keyword =
	{
		if (isCaseClass)
			"case class"
		else if ((properties ++ methods).exists { _.isAbstract })
			"abstract class"
		else
			"class"
	}
	
	override protected def constructorParams = Some(constructionParams)
	
	override protected def makeCopy(visibility: Visibility, extensions: Vector[Extension], creationCode: Code,
	                                properties: Vector[PropertyDeclaration], methods: Set[MethodDeclaration],
	                                nested: Set[InstanceDeclaration], description: String, author: String,
	                                headerComments: Vector[String], since: DeclarationDate) =
		ClassDeclaration(name, constructionParams, extensions, creationCode, properties, methods, nested, visibility,
			description, author, headerComments, since, isCaseClass)
}
