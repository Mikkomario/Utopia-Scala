package utopia.citadel.coder.model.scala.declaration

import utopia.citadel.coder.model.scala.Visibility.Public
import utopia.citadel.coder.model.scala.declaration.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.{Code, Parameter, Parameters, Visibility}

/**
  * Used for declaring properties in scala code files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class PropertyDeclaration(declarationType: PropertyDeclarationType, name: String, code: Code,
                               visibility: Visibility = Public, implicitParams: Vector[Parameter] = Vector(),
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
	
	override protected def params =
		if (implicitParams.nonEmpty) Some(Parameters(implicits = implicitParams)) else None
}
