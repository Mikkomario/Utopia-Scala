package utopia.reflection.component.reach.template

import utopia.reflection.component.reach.hierarchy.ComponentHierarchy

/**
  * A common trait for classes that produce component factories. Component factories then wrap a component hierarchy
  * and offer utility constructors for component creation
  * @author Mikko Hilpinen
  * @since 11.10.2020, v2
  */
trait ComponentFactoryFactory[+F]
{
	/**
	  * Creates a new factory
	  * @param hierarchy The parent hierarchy for the created component(s)
	  * @return A new component factory
	  */
	def apply(hierarchy: ComponentHierarchy): F
}
