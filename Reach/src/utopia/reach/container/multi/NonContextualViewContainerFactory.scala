package utopia.reach.container.multi

import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.template.ReachComponent
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
trait NonContextualViewContainerFactory[+Container, -Top <: ReachComponent]
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
		apply(Open.manyUsing[F, C, Changing[Boolean], R](contentFactory)(fill)(hierarchy.top))
	
	/**
	  * Builds a new container which reflects the contents of a multi-value pointer.
	  * This is best used in combination with view-based components.
	  * @param pointer Pointer whose value is reflected in the container contents
	  * @param contentFactory A factory used for constructing the individual components
	  * @param construct A function that accepts
	  *                  1) an initialized component-creation factory and
	  *                  2) a pointer that contains the value to display on that component,
	  *                  and yields a new component
	  * @tparam A Type of values displayed on individual components
	  * @tparam F Type of component factories to use
	  * @return A new container
	  */
	def mapPointer[A, F](pointer: Changing[Seq[A]], contentFactory: Cff[F])(construct: (F, Changing[A]) => Top): Container =
		_mapPointer(pointer) { p => Open.using(contentFactory) { construct(_, p) } }
}
