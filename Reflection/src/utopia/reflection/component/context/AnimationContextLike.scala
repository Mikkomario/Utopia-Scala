package utopia.reflection.component.context

import scala.concurrent.duration.FiniteDuration

/**
  * A common trait for context definitions for animations
  * @author Mikko Hilpinen
  * @since 28.4.2020, v1.2
  */
trait AnimationContextLike
{
	/**
	  * @return Duration of a singular animation by default
	  */
	def animationDuration: FiniteDuration
	
	/**
	  * @return Whether fading should be used in animations
	  */
	def useFadingInAnimations: Boolean
}
