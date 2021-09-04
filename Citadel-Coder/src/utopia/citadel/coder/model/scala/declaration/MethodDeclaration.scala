package utopia.citadel.coder.model.scala.declaration

import utopia.citadel.coder.model.scala.Visibility.Public
import utopia.citadel.coder.model.scala.{Code, Parameters, Reference, Visibility}

object MethodDeclaration
{
	/**
	  * Creates a new method declaration
	  * @param name Method name
	  * @param codeReferences References made within the code (default = empty)
	  * @param visibility Method visibility (default = public)
	  * @param description Description of this method (default = empty)
	  * @param returnDescription Description of the return value of this method (default = empty)
	  * @param isOverridden Whether this method overrides a base member (default = false)
	  * @param params Method parameters (0-n)
	  * @param firstLine First line of code
	  * @param moreLines More lines of code (0-n)
	  * @return A new method
	  */
	def apply(name: String, codeReferences: Set[Reference] = Set(), visibility: Visibility = Public,
	          description: String = "", returnDescription: String = "",
	          isOverridden: Boolean = false)(params: Parameters = Parameters.empty)
	         (firstLine: String, moreLines: String*): MethodDeclaration =
		apply(visibility, name, params, Code(firstLine +: moreLines.toVector, codeReferences), description,
			returnDescription, isOverridden)
}

/**
  * Represents a scala method
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param visibility Visibility of this method
  * @param name Method name
  * @param parameters Parameters accepted by this method
  * @param code Code executed within this method
  * @param description Description of this method (may be empty)
  * @param returnDescription Description of the return value of this method (may be empty)
  * @param isOverridden Whether this method overrides a base version
  */
case class MethodDeclaration(visibility: Visibility, name: String, parameters: Parameters, code: Code,
                             description: String, returnDescription: String,
                             isOverridden: Boolean) extends FunctionDeclaration
{
	override def keyword = "def"
	
	override protected def params = Some(parameters)
}
