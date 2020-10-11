package utopia.reflection.component.reach.template

import utopia.reflection.component.reach.hierarchy.ComponentHierarchy

object Mixed extends ComponentFactoryFactory[Mixed]

/**
  * A factory for creating all kinds of component factories
  * @author Mikko Hilpinen
  * @since 11.10.2020, v2
  */
case class Mixed(parentHierarchy: ComponentHierarchy)
{
	def apply[F](factoryFactory: ComponentFactoryFactory[F]) = factoryFactory(parentHierarchy)
}
