package utopia.reach.component.factory

import utopia.reach.component.hierarchy.ComponentHierarchy

object FromGenericContextComponentFactoryFactory
{
	/**
	  * Type of component factory factory commonly used in contextual container builders
	  */
	type ContextualBuilderContentFactory[N, +F[X <: N] <: GenericContextualFactory[X, _ >: N, F]] =
		FromGenericContextComponentFactoryFactory[_ >: N, _, F]
}

/**
  * A factory that creates factories that can be enriched with component creation contexts
  * @author Mikko Hilpinen
  * @since 14.10.2020, v0.1
  */
trait FromGenericContextComponentFactoryFactory[Top, +F <: FromGenericContextFactory[Top, CF], +CF[X <: Top]]
	extends ComponentFactoryFactory[F]
{
	/**
	  * Creates a new contextual component factory
	  * @param parentHierarchy Component hierarchy that will host created component(s)
	  * @param context Component creation context
	  * @tparam N Type of component creation context
	  * @return A new contextual component creation factory
	  */
	def withContext[N <: Top](parentHierarchy: ComponentHierarchy, context: N): CF[N] =
		apply(parentHierarchy).withContext(context)
	
	/**
	  * Creates a new contextual component factory
	  * @param parentHierarchy Component hierarchy that will host created component(s)
	  * @param context Implicit Component creation context
	  * @tparam N Type of component creation context
	  * @return A new contextual component creation factory
	  */
	def contextual[N <: Top](parentHierarchy: ComponentHierarchy)(implicit context: N): CF[N] =
		withContext[N](parentHierarchy, context)
}
