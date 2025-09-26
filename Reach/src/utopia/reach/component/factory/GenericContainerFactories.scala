package utopia.reach.component.factory

import utopia.reach.component.factory.ComponentFactories.CF
import utopia.reach.component.factory.ContextualComponentFactories.CCF
import utopia.reach.component.hierarchy.ComponentHierarchy

import scala.language.implicitConversions

object GenericContainerFactories
{
	// TYPES    ----------------------------
	
	/**
	 * A type alias for [[GenericContainerFactories]]
	 */
	type GCF[-Top, +CF[N <: Top]] = GenericContainerFactories[Top, CF]
	/**
	  * Type alias for FromGenericContextComponentFactoryFactory
	  */
	@deprecated("Renamed to GCF", "v1.7")
	type Gccff[-N, +F[X <: N]] = GCF[N, F]
	
	
	// IMPLICIT ----------------------------
	
	// Implicitly wraps certain kinds of component creation factories
	implicit def wrap[Top, F[X <: Top]](factory: CF[FromGenericContextFactory[Top, F]]): GCF[Top, F] =
		new _GenericContainerFactories[Top, F](factory)
	
	
	// NESTED   ----------------------------
	
	private class _GenericContainerFactories[-Top, +F[X <: Top]](factory: CF[FromGenericContextFactory[Top, F]])
		extends GenericContainerFactories[Top, F]
	{
		override def withContext[N <: Top](hierarchy: ComponentHierarchy, context: N): F[N] =
			factory(hierarchy).withContext(context)
	}
}

/**
  * A factory that creates factories that can be enriched with component creation contexts
  * @tparam Top The most abstract accepted context type
 * @tparam CF The type of (generic) contextual component/container factory generated
 * @author Mikko Hilpinen
  * @since 14.10.2020, v0.1
  */
trait GenericContainerFactories[-Top, +CF[X <: Top]]
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
	def static[N <: Top]: CCF[N, CF[N]] = ContextualComponentFactories(withContext)
	
	
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
