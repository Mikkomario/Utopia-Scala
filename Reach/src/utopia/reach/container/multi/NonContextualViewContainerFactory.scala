package utopia.reach.container.multi

import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentCreationResult.SwitchableCreations
import utopia.reach.component.wrapper.ComponentWrapResult.SwitchableComponentsWrapResult
import utopia.reach.component.wrapper.Open

/**
  * Common trait for pre-initialized container factories that wrap multiple switchable components
  * and don't use contextual information
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  * @tparam Container The type of container yielded by this factory
  * @tparam Top       The highest accepted wrapped component type (typically ReachComponentLike)
  */
trait NonContextualViewContainerFactory[+Container, -Top <: ReachComponentLike]
	extends ViewContainerFactory[Container, Top]
{
	/**
	  * Builds a new container using a function that specifies the wrapped contents
	  * @param contentFactory A factory used for producing the container content
	  * @param fill           A function that accepts an iterator of initialized content factories and yields the
	  *                       contents to wrap.
	  *                       The contents should be returned in the same order as their factories acquired from the iterator.
	  * @tparam F Type of initialized content factory used
	  * @tparam C Type of created component
	  * @tparam R Type of additional component creation result
	  * @return The created container, created components and the additional result
	  */
	def build[F, C <: Top, R](contentFactory: Cff[F])
	                         (fill: Iterator[F] => SwitchableCreations[C, R]): SwitchableComponentsWrapResult[Container, C, R] =
		apply(Open.manyUsing[F, C, Changing[Boolean], R](contentFactory)(fill)(parentHierarchy.top))
}
