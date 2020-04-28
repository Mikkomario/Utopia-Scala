package utopia.reflection.component.context

import utopia.genesis.handling.mutable.ActorHandler

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
	
	/**
	  * @return Actor handler used for delivering action events, which are used in animations
	  */
	def actorHandler: ActorHandler
}
