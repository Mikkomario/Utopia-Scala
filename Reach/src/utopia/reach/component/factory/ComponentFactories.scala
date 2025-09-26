package utopia.reach.component.factory

import utopia.reach.component.hierarchy.ComponentHierarchy

import scala.language.implicitConversions

object ComponentFactories
{
	// TYPES    -------------------------
	
	/**
	 * Type alias for [[ComponentFactories]]
	 */
	type CF[+F] = ComponentFactories[F]
	/**
	  * Type alias for ComponentFactoryFactory
	  */
	@deprecated("Renamed to CF", "v1.7")
	type Cff[+F] = CF[F]
	
	
	// IMPLICIT -------------------------
	
	implicit def apply[F](f: ComponentHierarchy => F): ComponentFactories[F] = new _ComponentFactories[F](f)
	
	
	// NESTED   -------------------------
	
	private class _ComponentFactories[+F](f: ComponentHierarchy => F) extends ComponentFactories[F]
	{
		override def apply(hierarchy: ComponentHierarchy): F = f(hierarchy)
	}
}

/**
  * A common trait for classes that produce component factories. Component factories then wrap a component hierarchy
  * and offer utility constructors for component creation
  * @author Mikko Hilpinen
  * @since 11.10.2020, v0.1
  */
trait ComponentFactories[+F]
{
	/**
	  * Creates a new factory
	  * @param hierarchy The parent hierarchy for the created component(s)
	  * @return A new component factory
	  */
	def apply(hierarchy: ComponentHierarchy): F
}
