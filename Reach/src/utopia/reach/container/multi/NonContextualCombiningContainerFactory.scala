package utopia.reach.container.multi

import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.wrapper.ComponentCreationResult.ComponentsResult
import utopia.reach.component.wrapper.Open

/**
  * Common trait for pre-initialized container factories that wrap multiple static components
  * and don't use contextual information
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  * @tparam Container The type of container yielded by this factory
  * @tparam Top       The highest accepted wrapped component type (typically ReachComponentLike)
  */
trait NonContextualCombiningContainerFactory[+Container[C <: Top], -Top]
	extends CombiningContainerFactory[Container, Top]
{
	/**
	  * Builds a new container using a function that specifies the wrapped contents
	  * @param contentFactory A factory used for producing the container content
	  * @param fill           A function that accepts an initialized content factory and yields the contents to wrap
	  * @tparam F Type of initialized content factory used
	  * @tparam C Type of created component
	  * @tparam R Type of additional component creation result
	  * @return The created container, created components and the additional result
	  */
	def build[F, C <: Top, R](contentFactory: Cff[F])(fill: F => ComponentsResult[C, R]) =
		apply(Open.using(contentFactory)(fill)(parentHierarchy.top))
}
