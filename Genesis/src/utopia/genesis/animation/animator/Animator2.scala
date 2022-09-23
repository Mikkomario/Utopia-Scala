package utopia.genesis.animation.animator

import utopia.flow.collection.mutable.async.ResettableVolatileLazy
import utopia.genesis.graphics.Drawer3
import utopia.genesis.handling.Drawable2
import utopia.genesis.handling.mutable.Actor

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for drawing animations with or without transformations. Mutable.
  * @author Mikko Hilpinen
  * @since 18.8.2019, v2.1+
  * @tparam A Type of animation result that is also drawn
  */
abstract class Animator2[A] extends Actor with Drawable2
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
	private lazy val cached = ResettableVolatileLazy { apply(_progress / animationDuration) }
	
	
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
	
	/**
	  * Draws an item
	  * @param drawer Drawer used
	  * @param item Item that should be drawn
	  */
	protected def draw(drawer: Drawer3, item: A): Unit
	
	
	// COMPUTED	------------------------
	
	/**
	  * @return Current progress of this animation
	  */
	def progress = _progress / animationDuration
	
	/**
	  * Changes this animation's progress
	  * @param newProgress The new progress of this animation
	  */
	def progress_=(newProgress: Double) =
	{
		_progress = newProgress * animationDuration
		cached.reset()
	}
	
	/**
	  * @return Currently displayed item
	  */
	def current = cached.value
	
	
	// IMPLEMENTED	--------------------
	
	override def act(duration: FiniteDuration) =
	{
		// Advances animation progress
		val mod = speedModifier
		if (mod != 0)
		{
			val ad = animationDuration
			if (allowsRepeat)
			{
				_progress += duration * mod
				// Resets progress if past animation duration
				while (_progress > ad) { _progress -= ad }
				cached.reset()
			}
			else if (_progress < ad)
			{
				val proposedProgress = _progress + duration * mod
				if (proposedProgress > ad)
					_progress = ad
				else
					_progress = proposedProgress
				cached.reset()
			}
		}
	}
	
	override def draw(drawer: Drawer3): Unit = draw(drawer, cached.value)
	
	
	// OTHER	----------------------
	
	/**
	  * Resets the current animation progress
	  */
	def reset() =
	{
		_progress = Duration.Zero
		cached.reset()
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
