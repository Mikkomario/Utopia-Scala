package utopia.citadel.coder.model.scala

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
	override def keyword = declarationType.toScala
	
	override def references = code.references
}
