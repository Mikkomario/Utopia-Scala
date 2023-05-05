package utopia.reach.container.multi

import utopia.reach.component.wrapper.ComponentWrapResult.SwitchableComponentsWrapResult
import utopia.reach.component.wrapper.OpenComponent.SwitchableOpenComponents
import utopia.reach.container.ContainerFactory

/**
  * Common trait for initialized container factories that create containers that switch some of their content on or off
  * @author Mikko Hilpinen
  * @since 5.5.2023, v1.1
  */
trait ViewContainerFactory[+Container[C <: Top], -Top]
	extends ContainerFactory[Container, Top, SwitchableOpenComponents, SwitchableComponentsWrapResult]
