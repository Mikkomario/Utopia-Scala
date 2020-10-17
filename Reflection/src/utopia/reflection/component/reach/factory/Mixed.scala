package utopia.reflection.component.reach.factory

import utopia.reflection.component.context.BaseContextLike
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy

object Mixed extends ContextInsertableComponentFactoryFactory[BaseContextLike, Mixed, ContextualMixed]

/**
  * A factory for creating all kinds of component factories
  * @author Mikko Hilpinen
  * @since 11.10.2020, v2
  */
case class Mixed(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[BaseContextLike, ContextualMixed]
{
	def apply[F](factoryFactory: ComponentFactoryFactory[F]) = factoryFactory(parentHierarchy)
	
	override def withContext[N <: BaseContextLike](context: N) =
		ContextualMixed(parentHierarchy, context)
}

case class ContextualMixed[N](parentHierarchy: ComponentHierarchy, context: N)
	extends ContextualComponentFactory[N, BaseContextLike, ContextualMixed]
{
	// IMPLEMENTED	---------------------------
	
	override def withContext[C2 <: BaseContextLike](newContext: C2) = copy(context = newContext)
	
	
	// OTHER	-------------------------------
	
	def apply[F[X <: N] <: ContextualComponentFactory[X, _ >: N, F]]
	(factoryFactory: ContextInsertableComponentFactoryFactory[_ >: N, _, F]) =
		factoryFactory.withContext(parentHierarchy, context)
	
	def withoutContext[F](factoryFactory: ComponentFactoryFactory[F]) = factoryFactory(parentHierarchy)
}