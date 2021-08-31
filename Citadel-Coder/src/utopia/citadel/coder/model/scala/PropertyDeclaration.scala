package utopia.citadel.coder.model.scala

import utopia.citadel.coder.model.scala.PropertyDeclarationType.ComputedProperty
import utopia.citadel.coder.model.scala.Visibility.Public

/**
  * Used for declaring properties in scala code files
  * @author Mikko Hilpinen
  * @since 30.8.2021, v0.1
  */
case class PropertyDeclaration(declarationType: PropertyDeclarationType, name: String, code: Code,
                               visibility: Visibility = Public, isOverridden: Boolean = false)
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
	
	override def references = code.references
	
	override def parametersString = ""
}
