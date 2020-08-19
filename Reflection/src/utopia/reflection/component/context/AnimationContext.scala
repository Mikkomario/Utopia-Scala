package utopia.reflection.component.context

import scala.concurrent.duration.FiniteDuration
import utopia.flow.util.TimeExtensions._
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.util.Fps

/**
  * A context definition for animations
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
case class AnimationContext(actorHandler: ActorHandler, animationDuration: FiniteDuration = 0.25.seconds,
							maxAnimationRefreshRate: Fps = Fps(120), useFadingInAnimations: Boolean = true)
	extends AnimationContextLike