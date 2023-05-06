package utopia.reach.container.wrapper

import utopia.reach.component.wrapper.{ComponentWrapResult, OpenComponent}
import utopia.reach.container.ContainerFactory

/**
  * Common trait for pre-initialized container factories that wrap a single component
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  * @tparam Container The type of container yielded by this factory
  * @tparam Top The highest accepted wrapped component type (typically ReachComponentLike)
  */
trait WrapperContainerFactory[+Container, -Top]
	extends ContainerFactory[Container, Top, OpenComponent, ComponentWrapResult]