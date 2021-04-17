package utopia.reach.component.factory

import utopia.reach.component.hierarchy.ComponentHierarchy

object Mixed extends ContextInsertableComponentFactoryFactory[Any, Mixed, ContextualMixed]

/**
  * A factory for creating all kinds of component factories
  * @author Mikko Hilpinen
  * @since 11.10.2020, v0.1
  */
case class Mixed(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[Any, ContextualMixed]
{
	override def withContext[N](context: N) = ContextualMixed(parentHierarchy, context)
	
	/**
	  * @param factoryFactory A component factory factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy
	  */
	def apply[F](factoryFactory: ComponentFactoryFactory[F]) = factoryFactory(parentHierarchy)
}

case class ContextualMixed[N](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, Any, ContextualMixed]
{
	// COMPUTED	-------------------------------
	
	/**
	  * @return A copy of this factory without any contextual information
	  */
	def withoutContext = Mixed(parentHierarchy)
	
	
	// IMPLEMENTED	---------------------------
	
	override def withContext[C2](newContext: C2) = copy(context = newContext)
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param factoryFactory A component factory factory
	  * @tparam F Type of component factory
	  * @return A specific type of component factory that uses this same hierarchy and context
	  */
	def apply[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
	(factoryFactory: ContextInsertableComponentFactoryFactory[_ >: N, _, F]) =
		factoryFactory.withContext(parentHierarchy, context)
}