package utopia.reach.component.factory

import utopia.reach.component.hierarchy.ComponentHierarchy

import scala.language.implicitConversions

object ComponentFactoryFactory
{
	// IMPLICIT -------------------------
	
	implicit def apply[F](f: ComponentHierarchy => F): ComponentFactoryFactory[F] = new _Cff[F](f)
	
	
	// NESTED   -------------------------
	
	private class _Cff[+F](f: ComponentHierarchy => F) extends ComponentFactoryFactory[F]
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
trait ComponentFactoryFactory[+F]
{
	/**
	  * Creates a new factory
	  * @param hierarchy The parent hierarchy for the created component(s)
	  * @return A new component factory
	  */
	def apply(hierarchy: ComponentHierarchy): F
}
