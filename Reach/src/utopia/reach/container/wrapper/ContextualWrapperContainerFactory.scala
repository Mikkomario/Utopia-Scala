package utopia.reach.container.wrapper

import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.GenericContextualFactory
import utopia.reach.component.wrapper.{ComponentCreationResult, Open}

/**
  * Common trait for initialized container factories that wrap a single component and use a component creation context
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  * @tparam N Type of context used by this container instance
  * @tparam TopN The highest context type that is allowed
  * @tparam Container Type of containers created using this factory
  * @tparam TopC Highest accepted component to wrap (typically ReachComponentLike)
  * @tparam Repr This factory type
  */
trait ContextualWrapperContainerFactory[+N, TopN, +Container[C <: TopC], -TopC, +Repr[N2 <: TopN]]
	extends WrapperContainerFactory[Container, TopC] with GenericContextualFactory[N, TopN, Repr]
{
	/**
	  * Builds a new container using a content-producing function
	  * @param contentFactory Type of factory used for creating the contents for this factory
	  * @param fill A function that accepts an initialized content factory and yields the component to wrap
	  * @tparam F Type of initialized content factory used
	  * @tparam C Type of component to wrap
	  * @tparam R Type of additional component creation result
	  * @return A new container (als includes the created component and the additional creation result)
	  */
	def build[F, C <: TopC, R](contentFactory: Ccff[N, F])(fill: F => ComponentCreationResult[C, R]) =
		apply[C, R](Open.withContext(context)(contentFactory)(fill)(parentHierarchy.top))
}
