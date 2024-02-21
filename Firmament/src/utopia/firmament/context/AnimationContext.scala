package utopia.firmament.context

import utopia.genesis.handling.action.ActorHandler
import utopia.genesis.util.Fps

import scala.concurrent.duration.FiniteDuration

object AnimationContext
{
	// OTHER    -----------------------
	
	/**
	  * @param actorHandler Actor handler that distributes action events for animations
	  * @param animationDuration Duration how long animations should last (default = common default)
	  * @param maxAnimationRefreshRate Animation refresh rate (default = common default)
	  * @param useFadingInAnimations Whether animations should use fading effects, when available (default = true)
	  * @return A new animation context instance
	  */
	def apply(actorHandler: ActorHandler,
	          animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
	          maxAnimationRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate,
	          useFadingInAnimations: Boolean = true): AnimationContext =
		_AnimationContext(actorHandler, animationDuration, maxAnimationRefreshRate, useFadingInAnimations)
	
	
	// NESTED   -----------------------
	
	private case class _AnimationContext(actorHandler: ActorHandler,
	                                     animationDuration: FiniteDuration = ComponentCreationDefaults.transitionDuration,
	                                     maxAnimationRefreshRate: Fps = ComponentCreationDefaults.maxAnimationRefreshRate,
	                                     useFadingInAnimations: Boolean = true)
		extends AnimationContext
}

/**
  * A common trait for context definitions for animations
  * @author Mikko Hilpinen
  * @since 28.4.2020, Reflection v1.2
  */
trait AnimationContext
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
	  * @return Maximum refresh rate to use when updating transitions or animations
	  */
	def maxAnimationRefreshRate: Fps
	
	/**
	  * @return Actor handler used for delivering action events, which are used in animations
	  */
	def actorHandler: ActorHandler
}