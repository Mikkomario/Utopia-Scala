package utopia.reach.component.factory

import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.hierarchy.ComponentHierarchy

import scala.language.implicitConversions

object FromGenericContextComponentFactoryFactory
{
	// TYPES    ----------------------------
	
	/**
	  * Type alias for FromGenericContextComponentFactoryFactory
	  */
	type Gccff[-N, +F[X <: N]] = FromGenericContextComponentFactoryFactory[N, F]
	
	/**
	  * Type of component factory factory commonly used in contextual container builders
	  */
	@deprecated("Deprecated for removal", "v1.0")
	type ContextualBuilderContentFactory[-N, +F[X <: N]/* <: GenericContextualFactory[X, _ >: N, F]*/] =
		FromGenericContextComponentFactoryFactory[N, F]
	
	
	// IMPLICIT ----------------------------
	
	// Implicitly wraps certain kinds of component creation factories
	implicit def wrap[Top, F[X <: Top]](factory: Cff[FromGenericContextFactory[Top, F]]): Gccff[Top, F] =
		new _Gcff[Top, F](factory)
	
	
	// NESTED   ----------------------------
	
	private class _Gcff[-Top, +F[X <: Top]](factory: ComponentFactoryFactory[FromGenericContextFactory[Top, F]])
		extends FromGenericContextComponentFactoryFactory[Top, F]
	{
		override def withContext[N <: Top](hierarchy: ComponentHierarchy, context: N): F[N] =
			factory(hierarchy).withContext(context)
	}
}

/**
  * A factory that creates factories that can be enriched with component creation contexts
  * @author Mikko Hilpinen
  * @since 14.10.2020, v0.1
  */
trait FromGenericContextComponentFactoryFactory[-Top, +CF[X <: Top]]
{
	// ABSTRACT ------------------------
	
	/**
	  * Creates a new contextual component factory
	  * @param hierarchy Component hierarchy that will host created component(s)
	  * @param context Component creation context
	  * @tparam N Type of component creation context
	  * @return A new contextual component creation factory
	  */
	def withContext[N <: Top](hierarchy: ComponentHierarchy, context: N): CF[N]
	
	
	// COMPUTED -----------------------
	
	/**
	  * @tparam N Type of highest accepted context
	  * @return A copy of this factory that yields factories wrapping that context type
	  */
	def static[N <: Top]: Ccff[N, CF[N]] = FromContextComponentFactoryFactory(withContext)
	
	
	// OTHER    -----------------------
	
	/**
	  * Creates a new contextual component factory
	  * @param hierarchy Component hierarchy that will host created component(s)
	  * @param context Implicit Component creation context
	  * @tparam N Type of component creation context
	  * @return A new contextual component creation factory
	  */
	def contextual[N <: Top](hierarchy: ComponentHierarchy)(implicit context: N): CF[N] =
		withContext[N](hierarchy, context)
}
