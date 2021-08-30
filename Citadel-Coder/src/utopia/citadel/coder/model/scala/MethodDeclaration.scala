package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.Visibility.Public

/**
  * Represents a scala method
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  * @param name Method name
  * @param parameters Parameters accepted by this method
  * @param code Code executed within this method
  * @param visibility Visibility of this method (default = public)
  * @param isOverridden Whether this method overrides a base version (default = false)
  */
case class MethodDeclaration(name: String, parameters: Vector[Parameter], code: Code,
                             visibility: Visibility = Public, isOverridden: Boolean = false) extends FunctionDeclaration
{
	override def keyword = "def"
	
	override def references = code.references ++ parameters.flatMap { _.references }
}
