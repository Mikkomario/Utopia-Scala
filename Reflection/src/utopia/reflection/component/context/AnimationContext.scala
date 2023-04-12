package utopia.reflection.component.context

import utopia.firmament.context.ComponentCreationDefaults
import scala.concurrent.duration.FiniteDuration
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.Fps

/**
  * A context definition for animations
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
@deprecated("Moved to Firmament", "v2.0")
case class AnimationContext(actorHandler: ActorHandler,
							animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
							maxAnimationRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate,
							useFadingInAnimations: Boolean = true)
	extends AnimationContextLike