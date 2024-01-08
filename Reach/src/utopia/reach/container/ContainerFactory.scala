package utopia.reach.container

import utopia.reach.component.template.PartOfComponentHierarchy

/**
  * Common trait for pre-initialized container factories
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  * @tparam Container The type of container yielded by this factory
  * @tparam Top The highest accepted wrapped component type (typically ReachComponentLike)
  * @tparam Content The type of content accepted by this container constructor.
  *                 Typically some variant of OpenComponent.
  *                 Has two generic parameters:
  *                     1) Type of component or components to wrap, and
  *                     2) Additional creation result type
  * @tparam Result The format in which the created container is returned.
  *                Typically some variant of ComponentWrapResult.
  *                Accepts 3 generic type parameters:
  *                     1) Created container,
  *                     2) Type of created component(s), and
  *                     3) Additional result type
  */
trait ContainerFactory[+Container, -Top, -Content[_, _], +Result[+_, _, _]] extends PartOfComponentHierarchy
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param content The content to wrap within this container (as a creation result)
	  * @tparam C Type of the wrapped component
	  * @tparam R Type of the additional component creation result
	  * @return A component wrapping result that contains
	  *             1) The created container,
	  *             2) The wrapped component, and
	  *             3) The additional result from the content
	  */
	def apply[C <: Top, R](content: Content[C, R]): Result[Container, C, R]
}
