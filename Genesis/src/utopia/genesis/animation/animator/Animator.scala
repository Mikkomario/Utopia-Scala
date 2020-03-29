package utopia.genesis.animation.animator

import utopia.flow.async.VolatileLazy
import utopia.genesis.handling.mutable.{Actor, Drawable}
import utopia.genesis.util.Drawer

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for drawing animations with or without transformations. Mutable.
  * @author Mikko Hilpinen
  * @since 18.8.2019, v2.1+
  * @tparam A Type of animation result that is also drawn
  */
trait Animator[A] extends Actor with Drawable
{
	// TODO: Add animation events
	// ATTRIBUTES	-------------------
	
	/**
	  * Modifier used for this animator's animation speed. Defaults to 1.0
	  */
	var speedModifier = 1.0
	private var _progress: Duration = Duration.Zero
	private lazy val cached = VolatileLazy { apply(_progress / animationDuration) }
	
	
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
	protected def draw(drawer: Drawer, item: A): Unit
	
	
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
	
	
	// IMPLEMENTED	--------------------
	
	override def act(duration: FiniteDuration) =
	{
		// Advances animation progress
		val mod = speedModifier
		if (mod != 0)
		{
			_progress += duration * mod
			val ad = animationDuration
			// Resets progress if past animation duration
			while (_progress > ad) { _progress -= ad }
			cached.reset()
		}
	}
	
	override def draw(drawer: Drawer): Unit = draw(drawer, cached.get)
	
	
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
}
