package utopia.reflection.component.reach.factory

import utopia.reflection.component.reach.factory.ContextInsertableComponentFactoryFactory.ContextualBuilderContentFactory

/**
  * A contextual container factory which in practice is only used for contextual builder creation
  * @author Mikko Hilpinen
  * @since 16.12.2020, v2
  * @tparam N Type of context being used
  * @tparam CF Type of container factory being wrapped
  * @tparam B Type of builder produced
  * @tparam Repr Implementation of this trait
  */
trait AnyContextContainerBuilderFactory[N, +CF, +B[BN, BF[X] <: ContextualComponentFactory[X, _ >: BN, BF]], +Repr[X]]
	extends ContextualComponentFactory[N, Any, Repr]
{
	// ABSTRACT	---------------------------------
	
	/**
	  * @return A copy of this factory without contextual information
	  */
	def withoutContext: CF
	
	/**
	  * Creates a new contextual container builder
	  * @param contentFactory Factory used for producing container content factories
	  * @tparam F Type of desired content factory
	  * @return A new builder
	  */
	def build[F[X] <: ContextualComponentFactory[X, _ >: N, F]](contentFactory: ContextualBuilderContentFactory[N, F]): B[N, F]
}
