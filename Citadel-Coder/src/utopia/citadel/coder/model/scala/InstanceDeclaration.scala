package utopia.citadel.coder.model.scala

/**
  * Declares an object or a class
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
trait InstanceDeclaration extends Declaration
{
	/**
	  * @return Classes & traits this instance extends, including possible construction parameters etc.
	  */
	def extensions: Vector[Extension]
	
	/**
	  * @return Code executed every time an instance is created (optional)
	  */
	def creationCode: Option[Code]
	
	/**
	  * @return Properties defined in this instance
	  */
	def properties: Set[PropertyDeclaration]
	
	/**
	  * @return Methods defined for this instance
	  */
	def methods: Set[MethodDeclaration]
}
