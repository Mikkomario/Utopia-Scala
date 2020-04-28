package utopia.reflection.component.context

import scala.concurrent.duration.FiniteDuration

import utopia.flow.util.TimeExtensions._

object AnimationContext
{
	/**
	  * The default context for animations
	  */
	val default = AnimationContext()
}

/**
  * A context definition for animations
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
case class AnimationContext(animationDuration: FiniteDuration = 0.25.seconds, useFadingInAnimations: Boolean = true)
	extends AnimationContextLike