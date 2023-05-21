package utopia.reach.coder.model.data

import utopia.coder.model.data.Name
import utopia.coder.model.scala.Package
import utopia.reach.coder.model.enumeration.{ContextType, ReachFactoryTrait}

/**
  * Used for declaring component factories and related classes
  * @author Mikko Hilpinen
  * @since 19.5.2023, v1.0
  * @constructor Declares a new component factory
  * @param pck Package within which this component / file appears
  * @param componentName Name of the created component. E.g. "Label"
  * @param contextType The type of context utilized by this factory, if applicable
  * @param parentTraits Traits extended by this factory type (including all settings classes)
  * @param properties Properties used by all settings and factory classes
  * @param nonContextualProperties Properties that appear only in non-contextual factory variants
  * @param contextualProperties Properties that only appear in contextual factory variants
  * @param author Author of this component (may be empty)
  * @param onlyContextual Whether only the contextual factories are used
  */
case class ComponentFactory(pck: Package, componentName: Name, contextType: Option[ContextType] = None,
                            parentTraits: Vector[ReachFactoryTrait] = Vector(),
                            properties: Vector[Property] = Vector(),
                            nonContextualProperties: Vector[Property] = Vector(),
                            contextualProperties: Vector[Property] = Vector(),
                            author: String = "", onlyContextual: Boolean = false)
{
	/**
	  * @return All properties declared / used by this factory
	  */
	def allProperties = parentTraits.map { _.property } ++ properties
}