package utopia.reach.container.multi

import utopia.reach.component.wrapper.ComponentWrapResult.ComponentsWrapResult
import utopia.reach.component.wrapper.OpenComponent.BundledOpenComponents
import utopia.reach.container.ContainerFactory

/**
  * Common trait for container factories which wrap multiple components at once,
  * using the same hierarchy for each. This is typically the case for immutable multi-containers.
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  * @tparam Container The type of container yielded by this factory
  * @tparam Top       The highest accepted wrapped component type (typically ReachComponentLike)
  */
trait CombiningContainerFactory[+Container, -Top]
	extends ContainerFactory[Container, Top, BundledOpenComponents, ComponentsWrapResult]
