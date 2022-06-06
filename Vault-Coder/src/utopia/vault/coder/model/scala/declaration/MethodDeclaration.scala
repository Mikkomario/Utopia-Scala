package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.merging.Mergeable
import utopia.vault.coder.model.scala.code.Code
import utopia.vault.coder.model.scala.Visibility.Public
import utopia.vault.coder.model.scala.datatype.{GenericType, Reference, ScalaType}
import utopia.vault.coder.model.scala.{Parameters, Visibility}

object MethodDeclaration
{
	/**
	  * Creates a new method declaration
	  * @param name Method name
	  * @param codeReferences References made within the code (default = empty)
	  * @param visibility Method visibility (default = public)
	  * @param genericTypes Generic types to use within this method (default = empty)
	  * @param explicitOutputType Data type returned by this method, when explicitly defined (optional)
	  * @param description Description of this method (default = empty)
	  * @param returnDescription Description of the return value of this method (default = empty)
	  * @param headerComments Lines of comments to insert before the declaration (default = empty)
	  * @param isOverridden Whether this method overrides a base member (default = false)
	  * @param params Method parameters (0-n)
	  * @param firstLine First line of code
	  * @param moreLines More lines of code (0-n)
	  * @return A new method
	  */
	def apply(name: String, codeReferences: Set[Reference] = Set(), visibility: Visibility = Public,
	          genericTypes: Seq[GenericType] = Vector(), explicitOutputType: Option[ScalaType] = None,
	          description: String = "", returnDescription: String = "", headerComments: Vector[String] = Vector(),
	          isOverridden: Boolean = false, isLowMergePriority: Boolean = false)
	         (params: Parameters = Parameters.empty)
	         (firstLine: String, moreLines: String*): MethodDeclaration =
		apply(visibility, name, genericTypes, params,
			Code.from(firstLine +: moreLines.toVector).referringTo(codeReferences),
			explicitOutputType, description, returnDescription, headerComments, isOverridden, isLowMergePriority)
}

/**
  * Represents a scala method
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param visibility Visibility of this method
  * @param name Method name
  * @param genericTypes Generic types to declare within this method
  * @param parameters Parameters accepted by this method
  * @param bodyCode Code executed within this method
  * @param explicitOutputType Data type returned by this method, when explicitly defined (optional)
  * @param description Description of this method (may be empty)
  * @param returnDescription Description of the return value of this method (may be empty)
  * @param headerComments Lines of comments to insert before the declaration (default = empty)
  * @param isOverridden Whether this method overrides a base version
  */
case class MethodDeclaration(visibility: Visibility, name: String, genericTypes: Seq[GenericType],
                             parameters: Parameters, bodyCode: Code, explicitOutputType: Option[ScalaType],
                             description: String, returnDescription: String,
                             headerComments: Vector[String], isOverridden: Boolean, isLowMergePriority: Boolean)
	extends FunctionDeclaration[MethodDeclaration] with Mergeable[MethodDeclaration, MethodDeclaration]
{
	override def keyword = "def"
	
	override protected def params = Some(parameters)
	
	override protected def makeCopy(visibility: Visibility, genericTypes: Seq[GenericType],
	                                parameters: Option[Parameters], bodyCode: Code,
	                                explicitOutputType: Option[ScalaType], description: String,
	                                returnDescription: String, headerComments: Vector[String],
	                                isOverridden: Boolean) =
		MethodDeclaration(visibility, name, genericTypes, parameters.getOrElse(this.parameters), bodyCode,
			explicitOutputType, description, returnDescription, headerComments, isOverridden, isLowMergePriority)
}
