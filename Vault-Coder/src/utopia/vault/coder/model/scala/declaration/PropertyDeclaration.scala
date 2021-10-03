package utopia.vault.coder.model.scala.declaration

import utopia.vault.coder.model.scala.Visibility.{Protected, Public}
import utopia.vault.coder.model.scala.{Code, Parameter, Parameters, ScalaType, Visibility}
import utopia.vault.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty

object PropertyDeclaration
{
	/**
	  * Creates a new abstract computed property
	  * @param name Name of this property
	  * @param outputType Data type of this property
	  * @param implicitParams Implicit parameters accepted (default = empty)
	  * @param description Description of this property (default = empty)
	  * @param isProtected Whether this property should be protected instead of public (default = false)
	  * @param isOverridden Whether this property overrides a base member (default = false)
	  * @return A new property declaration
	  */
	def newAbstract(name: String, outputType: ScalaType, implicitParams: Vector[Parameter] = Vector(),
	                description: String = "", isProtected: Boolean = false, isOverridden: Boolean = false) =
		apply(ComputedProperty, name, Code.empty, if (isProtected) Protected else Public, Some(outputType),
			description, implicitParams, isOverridden)
}

/**
  * Used for declaring properties in scala code files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class PropertyDeclaration(declarationType: PropertyDeclarationType, name: String, bodyCode: Code,
                               visibility: Visibility = Public, explicitOutputType: Option[ScalaType] = None,
                               description: String = "", implicitParams: Vector[Parameter] = Vector(),
                               isOverridden: Boolean = false)
	extends FunctionDeclaration
{
	// COMPUTED -------------------------------------------
	
	/**
	  * @return Whether this is a computed property
	  */
	def isComputed = declarationType == ComputedProperty
	
	/**
	  * @return Whether the value of this property is stored (not computed)
	  */
	def isStored = !isComputed
	
	
	// IMPLEMENTED  ---------------------------------------
	
	override def keyword = declarationType.toScala
	
	override def returnDescription = ""
	
	override protected def params =
		if (implicitParams.nonEmpty) Some(Parameters(implicits = implicitParams)) else None
}
