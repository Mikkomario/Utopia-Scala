package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.Visibility.Public

object MethodDeclaration
{
	/**
	  * Creates a new method declaration
	  * @param name Method name
	  * @param codeReferences References made within the code (default = empty)
	  * @param visibility Method visibility (default = public)
	  * @param isOverridden Whether this method overrides a base member (default = false)
	  * @param params Method parameters (0-n)
	  * @param firstLine First line of code
	  * @param moreLines More lines of code (0-n)
	  * @return A new method
	  */
	def apply(name: String, codeReferences: Set[Reference] = Set(), visibility: Visibility = Public,
	          isOverridden: Boolean = false)(params: Parameter*)
	         (firstLine: String, moreLines: String*): MethodDeclaration =
		apply(visibility, name, params.toVector, Code(firstLine +: moreLines.toVector, codeReferences), isOverridden)
}

/**
  * Represents a scala method
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param visibility Visibility of this method
  * @param name Method name
  * @param parameters Parameters accepted by this method
  * @param code Code executed within this method
  * @param isOverridden Whether this method overrides a base version (default = false)
  */
case class MethodDeclaration(visibility: Visibility, name: String, parameters: Vector[Parameter], code: Code,
                             isOverridden: Boolean) extends FunctionDeclaration
{
	override def keyword = "def"
	
	override def references = code.references ++ parameters.flatMap { _.references }
	
	override def parametersString = "(" + parameters.map { _.toScala }.mkString(", ") + ')'
}
