package utopia.genesis.handling.event.animation

import utopia.genesis.handling.event.animation.AnimationListener.AnimationEventFilter

/**
  * Common trait for events fired during animation-processing
  * @author Mikko Hilpinen
  * @since 22/02/2024, v4.0
  */
sealed trait AnimationEvent
{
	// ABSTRACT --------------------
	
	/**
	  * @return Current animation progress. Between 0 (start) and 1 (end).
	  */
	def progress: Double
	
	/**
	  * @return Whether the animation will continue after this event
	  */
	def continues: Boolean
	
	
	// COMPUTED -------------------
	
	/**
	  * @return Whether the animation will not progress forward after this event
	  */
	def stops = !continues
}

object AnimationEvent
{
	// COMPUTED  -----------------
	
	/**
	  * @return Access to filters that may be applied to these events
	  */
	def filter = AnimationEventFilter
	
	
	// VALUES   ------------------
	
	/**
	  * An event fired when an animation first starts running, continues after being stopped,
	  * or is restarted from the beginning without completing first.
	  */
	case class Started(from: Double = 0.0) extends AnimationEvent
	{
		override def progress: Double = from
		override def continues: Boolean = true
	}
	
	/**
	  * An event fired when an animation is paused or stopped at some state other than its completion.
	  * Also fired if the animation is moved to a different state without continuing it.
	  * @param progress Animation progress [0,1] where the animation was paused
	  */
	case class Paused(progress: Double) extends AnimationEvent
	{
		override def continues: Boolean = false
	}
	
	/**
	  * An event fired when an animation completes
	  * @param loops Whether the animation will start again afterwards.
	  */
	case class Completed(loops: Boolean) extends AnimationEvent
	{
		// IMPLEMENTED  -------------------
		
		override def progress: Double = 1.0
		override def continues: Boolean = loops
	}
}
