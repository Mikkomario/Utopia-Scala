package utopia.reach.container.multi

import utopia.flow.view.template.eventful.Changing
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.GenericContextualFactory
import utopia.reach.component.template.ReachComponentLike
import utopia.reach.component.wrapper.ComponentCreationResult.SwitchableCreations
import utopia.reach.component.wrapper.Open

/**
  * Common trait for initialized container factories that wrap multiple switchable components
  * and use a component creation context
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  * @tparam N Type of context used by this container instance
  * @tparam TopN The highest context type that is allowed
  * @tparam Container Type of containers created using this factory
  * @tparam TopC Highest accepted component to wrap (typically ReachComponentLike)
  * @tparam Repr This factory type
  */
trait ContextualViewContainerFactory[+N, TopN, +Container, -TopC <: ReachComponentLike, +Repr[N2 <: TopN]]
	extends ViewContainerFactory[Container, TopC] with GenericContextualFactory[N, TopN, Repr]
{
	/**
	  * Builds a new container using a content-producing function
	  * @param contentFactory Type of factory used for creating the contents for this factory
	  * @param fill A function that accepts an initialized content factory and yields the components to wrap
	  * @tparam F Type of initialized content factory used
	  * @tparam C Type of component to wrap
	  * @tparam R Type of additional component creation result
	  * @return A new container (also includes the created component and the additional creation result)
	  */
	def build[F, C <: TopC, R](contentFactory: Ccff[N, F])(fill: Iterator[F] => SwitchableCreations[C, R]) =
		apply(Open.withContext(context).many(contentFactory)(fill)(parentHierarchy.top))
	
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
	def mapPointer[A, F](pointer: Changing[Seq[A]], contentFactory: Ccff[N, F])(construct: (F, Changing[A]) => TopC) =
		_mapPointer(pointer) { p => Open.withContext(context)(contentFactory) { construct(_, p) } }
}
