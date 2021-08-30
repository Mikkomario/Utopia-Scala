package utopia.citadel.coder.model.scala

/**
  * Declares a scala method or a property of some sort
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait FunctionDeclaration extends Declaration
{
	/**
	  * @return Code that forms this method / property
	  */
	def code: Code
	
	/**
	  * @return Whether this declaration overrides a base declaration
	  */
	def isOverridden: Boolean
}
