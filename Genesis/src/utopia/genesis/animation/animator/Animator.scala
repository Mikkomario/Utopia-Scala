package utopia.genesis.animation.animator

import utopia.flow.util.logging.SysErrLogger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.CopyOnDemand
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.action.Actor

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for generating time-based animations. Mutable.
  * @author Mikko Hilpinen
  * @since 18.8.2019, v2.1+
  * @tparam A Type of animation result
  */
@deprecated("Deprecated for removal. Replaced with a new version.", "v4.0")
abstract class Animator[A] extends Actor
{
	// TODO: Add animation events
	// ATTRIBUTES	-------------------
	
	/**
	  * Modifier used for this animator's animation speed. Defaults to 1.0
	  */
	var speedModifier = 1.0
	/**
	  * Whether drawn animation is allowed to automatically repeat. Defaults to true.
	  */
	var allowsRepeat = true
	private var _progress: Duration = Duration.Zero
	
	private val _pointer = CopyOnDemand(View { apply(_progress / animationDuration) })(SysErrLogger)
	
	
	// ABSTRACT	-----------------------
	
	/**
	  * @return The (current) duration of the animation used by this animator
	  */
	def animationDuration: Duration
	/**
	  * @param progress Animation progress [0, 1]
	  * @return Animation result for the specified progress
	  */
	protected def apply(progress: Double): A
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Current progress of this animation
	  */
	def progress = _progress / animationDuration
	/**
	  * Changes this animation's progress
	  * @param newProgress The new progress of this animation
	  */
	def progress_=(newProgress: Double) = {
		_progress = newProgress * animationDuration
		_pointer.update()
	}
	
	/**
	  * @return A pointer that contains the currently displayed item
	  */
	def pointer = _pointer.readOnly
	/**
	  * @return Currently displayed item
	  */
	def current = _pointer.value
	
	
	// IMPLEMENTED	--------------------
	
	override def handleCondition: FlagLike = AlwaysTrue
	
	override def act(duration: FiniteDuration) = {
		// Advances animation progress
		val mod = speedModifier
		if (mod != 0) {
			val ad = animationDuration
			if (allowsRepeat) {
				_progress += duration * mod
				// Resets progress if past animation duration
				while (_progress > ad) { _progress -= ad }
				_pointer.update()
			}
			else if (_progress < ad) {
				val proposedProgress = _progress + duration * mod
				if (proposedProgress > ad)
					_progress = ad
				else
					_progress = proposedProgress
				_pointer.update()
			}
		}
	}
	
	
	// OTHER	----------------------
	
	/**
	  * Resets the current animation progress
	  */
	def reset() = {
		_progress = Duration.Zero
		_pointer.update()
	}
	
	/**
	  * Stops this animation by setting the speed modifier to 0
	  */
	def stop() = speedModifier = 0
	
	/**
	  * Makes it so that this animator will always stop at the end of each animation
	  */
	def disableRepeat() = allowsRepeat = false
	/**
	  * Makes it so that this animator will keep repeating the current animation
	  */
	def enableRepeat() = allowsRepeat = true
}
